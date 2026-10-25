package com.personal.journal.backup

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class SigV4Test {
    /** AWS SigV4 test suite "get-vanilla". */
    @Test
    fun matchesAwsGetVanilla() {
        val headers = SigV4.sign(
            method = "GET",
            url = "https://example.amazonaws.com/".toHttpUrl(),
            payloadSha256 = SigV4.EMPTY_SHA256,
            accessKeyId = "AKIDEXAMPLE",
            secretAccessKey = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY",
            now = Instant.parse("2015-08-30T12:36:00Z"),
            region = "us-east-1",
            service = "service",
            signPayloadHeader = false,
        )
        assertEquals("20150830T123600Z", headers["x-amz-date"])
        assertEquals(
            "AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20150830/us-east-1/service/aws4_request, " +
                "SignedHeaders=host;x-amz-date, Signature=5fa00fa31553b73ebf1942676e86291e8372ff2a2260956d9b8aae1d763fbf31",
            headers["Authorization"],
        )
    }

    @Test
    fun queryIsSortedAndEncoded() {
        val url = "https://h/b".toHttpUrl().newBuilder()
            .addQueryParameter("prefix", "entries/")
            .addQueryParameter("list-type", "2")
            .addQueryParameter("continuation-token", "a+b=")
            .build()
        val q = SigV4.canonicalQuery(url)
        assertEquals("continuation-token=a%2Bb%3D&list-type=2&prefix=entries%2F", q)
    }

    @Test
    fun pathKeepsSlashes() {
        assertEquals("/bucket/entries/abc.json.enc", SigV4.canonicalPath("https://h/bucket/entries/abc.json.enc".toHttpUrl()))
    }
}
