package com.personal.journal.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.time.Clock

enum class BackupErrorKind(val message: String) {
    AUTH("The R2 keys were rejected. Check the Access Key ID and Secret."),
    BUCKET_NOT_FOUND("The bucket wasn't found. Check its name."),
    NETWORK("No connection to R2. It will retry on the next backup."),
    STORAGE_FULL("The R2 bucket is out of space."),
    WRONG_KEY("This bucket has a backup made with a different recovery phrase."),
    UNKNOWN("R2 returned an unexpected error."),
}

class BackupException(val kind: BackupErrorKind, cause: Throwable? = null) : IOException(kind.message, cause) {
    /** Wording for setup and restore, where the user is typing the details right now. */
    fun setupMessage(): String =
        if (kind == BackupErrorKind.NETWORK) "Couldn't reach R2. Check the Account ID and your internet connection." else kind.message
}

/** The four object operations backup needs; R2Client is the real one, tests use an in-memory fake. */
interface ObjectStore {
    suspend fun put(key: String, bytes: ByteArray)
    suspend fun get(key: String): ByteArray?
    suspend fun delete(key: String)
    suspend fun list(prefix: String): List<String>
}

class R2Client(
    private val http: OkHttpClient,
    private val creds: R2Credentials,
    private val endpoint: HttpUrl = "https://${creds.accountId}.r2.cloudflarestorage.com".toHttpUrl(),
    private val clock: Clock = Clock.systemUTC(),
) : ObjectStore {

    private fun url(key: String? = null): HttpUrl.Builder =
        endpoint.newBuilder().addPathSegment(creds.bucket).apply { key?.split("/")?.forEach { addPathSegment(it) } }

    private suspend fun call(method: String, url: HttpUrl, body: ByteArray? = null): Response = withContext(Dispatchers.IO) {
        val payload = body ?: ByteArray(0)
        val signed = SigV4.sign(method, url, SigV4.sha256Hex(payload), creds.accessKeyId, creds.secretAccessKey, clock.instant())
        val request = Request.Builder().url(url).apply {
            signed.forEach { (k, v) -> header(k, v) }
            method(method, if (method == "PUT") payload.toRequestBody() else null)
        }.build()
        try {
            http.newCall(request).execute()
        } catch (e: IOException) {
            throw BackupException(BackupErrorKind.NETWORK, e)
        }
    }

    private fun fail(r: Response): Nothing {
        val body = r.body.string()
        r.close()
        throw BackupException(
            when {
                r.code == 401 || r.code == 403 -> BackupErrorKind.AUTH
                body.contains("NoSuchBucket") -> BackupErrorKind.BUCKET_NOT_FOUND
                body.contains("QuotaExceeded") || r.code == 507 -> BackupErrorKind.STORAGE_FULL
                else -> BackupErrorKind.UNKNOWN
            },
        )
    }

    override suspend fun put(key: String, bytes: ByteArray) {
        call("PUT", url(key).build(), bytes).use { if (!it.isSuccessful) fail(it) }
    }

    override suspend fun get(key: String): ByteArray? {
        val r = call("GET", url(key).build())
        if (r.code == 404 && !r.peekBody(4096).string().contains("NoSuchBucket")) {
            r.close()
            return null
        }
        if (!r.isSuccessful) fail(r)
        return r.use { it.body.bytes() }
    }

    override suspend fun delete(key: String) {
        call("DELETE", url(key).build()).use { if (!it.isSuccessful && it.code != 404) fail(it) }
    }

    override suspend fun list(prefix: String): List<String> {
        val keys = mutableListOf<String>()
        var token: String? = null
        do {
            val u = url().addQueryParameter("list-type", "2").addQueryParameter("prefix", prefix)
                .apply { token?.let { addQueryParameter("continuation-token", it) } }.build()
            val r = call("GET", u)
            if (!r.isSuccessful) fail(r)
            val xml = r.use { it.body.string() }
            keys += Regex("<Key>(.*?)</Key>").findAll(xml).map { unescape(it.groupValues[1]) }
            token = Regex("<NextContinuationToken>(.*?)</NextContinuationToken>").find(xml)?.groupValues?.get(1)?.let(::unescape)
            val truncated = xml.contains("<IsTruncated>true</IsTruncated>")
        } while (truncated && token != null)
        return keys
    }

    private fun unescape(s: String) =
        s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&")
}
