package com.personal.journal.backup

import okhttp3.HttpUrl
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Minimal AWS Signature Version 4 signer for R2's S3 API. */
object SigV4 {
    const val EMPTY_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    private val amzDate = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)
    private val day = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC)

    fun sha256Hex(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).hex()

    private fun ByteArray.hex() = joinToString("") { "%02x".format(it) }

    private fun hmac(key: ByteArray, data: String): ByteArray =
        Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(key, "HmacSHA256")) }.doFinal(data.toByteArray())

    /** RFC 3986 encoding as AWS expects: unreserved characters kept, everything else %XX. */
    fun encode(s: String, keepSlash: Boolean = false): String = buildString {
        for (b in s.toByteArray()) {
            val c = b.toInt().toChar()
            when {
                c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9' || c == '-' || c == '_' || c == '.' || c == '~' -> append(c)
                c == '/' && keepSlash -> append(c)
                else -> append("%%%02X".format(b.toInt() and 0xFF))
            }
        }
    }

    fun canonicalPath(url: HttpUrl): String =
        "/" + url.pathSegments.joinToString("/") { encode(it) }

    fun canonicalQuery(url: HttpUrl): String =
        (0 until url.querySize)
            .map { encode(url.queryParameterName(it)) to encode(url.queryParameterValue(it).orEmpty()) }
            .sortedWith(compareBy({ it.first }, { it.second }))
            .joinToString("&") { "${it.first}=${it.second}" }

    /** Returns the headers to add: Authorization, x-amz-date and (for S3) x-amz-content-sha256. */
    fun sign(
        method: String,
        url: HttpUrl,
        payloadSha256: String,
        accessKeyId: String,
        secretAccessKey: String,
        now: Instant,
        region: String = "auto",
        service: String = "s3",
        signPayloadHeader: Boolean = true,
    ): Map<String, String> {
        val date = amzDate.format(now)
        val host = if (url.port == HttpUrl.defaultPort(url.scheme)) url.host else "${url.host}:${url.port}"
        val headers = sortedMapOf("host" to host, "x-amz-date" to date)
        if (signPayloadHeader) headers["x-amz-content-sha256"] = payloadSha256
        val signedHeaders = headers.keys.joinToString(";")
        val canonical = listOf(
            method,
            canonicalPath(url),
            canonicalQuery(url),
            headers.entries.joinToString("") { "${it.key}:${it.value.trim()}\n" },
            signedHeaders,
            payloadSha256,
        ).joinToString("\n")
        val scope = "${day.format(now)}/$region/$service/aws4_request"
        val toSign = "AWS4-HMAC-SHA256\n$date\n$scope\n${sha256Hex(canonical.toByteArray())}"
        var k = hmac("AWS4$secretAccessKey".toByteArray(), day.format(now))
        k = hmac(k, region)
        k = hmac(k, service)
        k = hmac(k, "aws4_request")
        val signature = hmac(k, toSign).hex()
        return buildMap {
            put("Authorization", "AWS4-HMAC-SHA256 Credential=$accessKeyId/$scope, SignedHeaders=$signedHeaders, Signature=$signature")
            put("x-amz-date", date)
            if (signPayloadHeader) put("x-amz-content-sha256", payloadSha256)
        }
    }
}
