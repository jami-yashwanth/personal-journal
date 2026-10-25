package com.personal.journal.backup

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class R2ClientTest {
    private val server = MockWebServer()
    private lateinit var client: R2Client

    @Before
    fun setUp() {
        server.start()
        client = R2Client(
            OkHttpClient(), R2Credentials("acct", "AKID", "SECRET", "journal"),
            endpoint = server.url("/"), clock = Clock.fixed(Instant.parse("2026-10-25T10:00:00Z"), ZoneOffset.UTC),
        )
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun putSendsSignedRequest() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))
        client.put("entries/abc.json.enc", byteArrayOf(1, 2, 3))
        val r = server.takeRequest()
        assertEquals("PUT", r.method)
        assertEquals("/journal/entries/abc.json.enc", r.path)
        assertTrue(r.getHeader("Authorization")!!.startsWith("AWS4-HMAC-SHA256 Credential=AKID/20261025/auto/s3/aws4_request"))
        assertEquals(SigV4.sha256Hex(byteArrayOf(1, 2, 3)), r.getHeader("x-amz-content-sha256"))
        assertArrayEquals(byteArrayOf(1, 2, 3), r.body.readByteArray())
    }

    @Test
    fun getMissingReturnsNull() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("<Error><Code>NoSuchKey</Code></Error>"))
        assertNull(client.get("meta.json.enc"))
    }

    @Test
    fun listFollowsContinuation() = runTest {
        server.enqueue(MockResponse().setBody("<ListBucketResult><IsTruncated>true</IsTruncated><Contents><Key>entries/a.json.enc</Key></Contents><NextContinuationToken>t+1=</NextContinuationToken></ListBucketResult>"))
        server.enqueue(MockResponse().setBody("<ListBucketResult><IsTruncated>false</IsTruncated><Contents><Key>entries/b.json.enc</Key></Contents></ListBucketResult>"))
        assertEquals(listOf("entries/a.json.enc", "entries/b.json.enc"), client.list("entries/"))
        server.takeRequest()
        val second = server.takeRequest()
        assertEquals("t+1=", second.requestUrl!!.queryParameter("continuation-token"))
    }

    @Test
    fun status403MapsToAuth() = runTest {
        server.enqueue(MockResponse().setResponseCode(403).setBody("<Error><Code>AccessDenied</Code></Error>"))
        val e = runCatching { client.put("x", byteArrayOf()) }.exceptionOrNull() as BackupException
        assertEquals(BackupErrorKind.AUTH, e.kind)
    }

    @Test
    fun missingBucketMapsToBucketNotFound() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("<Error><Code>NoSuchBucket</Code></Error>"))
        val e = runCatching { client.get("meta.json.enc") }.exceptionOrNull() as BackupException
        assertEquals(BackupErrorKind.BUCKET_NOT_FOUND, e.kind)
    }

    @Test
    fun networkErrorMapsToNetwork() = runTest {
        server.shutdown()
        val e = runCatching { client.put("x", byteArrayOf()) }.exceptionOrNull() as BackupException
        assertEquals(BackupErrorKind.NETWORK, e.kind)
    }
}
