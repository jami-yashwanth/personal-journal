package com.personal.journal.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.SecureRandom

class CryptoTest {
    private val words = File("src/main/res/raw/bip39_english.txt").readLines().filter { it.isNotBlank() }
    private val phrase = RecoveryPhrase(words)

    private fun hex(s: String) = s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    @Test
    fun bip39Vectors() {
        assertEquals(
            "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about",
            phrase.fromEntropy(ByteArray(16)).joinToString(" "),
        )
        assertEquals(
            "legal winner thank year wave sausage worth useful legal winner thank yellow",
            phrase.fromEntropy(hex("7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f")).joinToString(" "),
        )
    }

    @Test
    fun phraseRoundTrip() {
        val w = phrase.generate(SecureRandom())
        assertEquals(12, w.size)
        val entropy = phrase.toEntropy(w)
        assertEquals(w, phrase.fromEntropy(entropy))
    }

    @Test
    fun phraseInputIsForgiving() {
        val w = phrase.fromEntropy(hex("7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f"))
        assertArrayEquals(phrase.toEntropy(w), phrase.toEntropy(phrase.parse("  LEGAL winner\nthank year wave sausage worth useful legal winner thank yellow ")))
    }

    @Test(expected = InvalidPhraseException::class)
    fun badChecksumRejected() {
        phrase.toEntropy("abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon".split(" "))
    }

    @Test(expected = InvalidPhraseException::class)
    fun unknownWordRejected() {
        phrase.toEntropy(List(11) { "abandon" } + "notaword")
    }

    @Test
    fun hkdfRfc5869Case1() {
        val okm = hkdfSha256(
            ikm = ByteArray(22) { 0x0b },
            salt = hex("000102030405060708090a0b0c"),
            info = hex("f0f1f2f3f4f5f6f7f8f9"),
            length = 42,
        )
        assertArrayEquals(hex("3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865"), okm)
    }

    @Test
    fun masterKeyIsStable() {
        val k1 = deriveMasterKey(ByteArray(16) { 1 })
        val k2 = deriveMasterKey(ByteArray(16) { 1 })
        assertArrayEquals(k1.encoded, k2.encoded)
        assertEquals(32, k1.encoded.size)
    }

    private val key = deriveMasterKey(ByteArray(16) { 7 })

    @Test
    fun sealOpenRoundTrip() {
        val sealed = Envelope.seal(key, "hello".toByteArray(), "entries/a.json.enc")
        assertEquals(Envelope.VERSION, sealed[0])
        assertEquals("hello", String(Envelope.open(key, sealed, "entries/a.json.enc")))
    }

    @Test(expected = EnvelopeException::class)
    fun wrongKeyFails() {
        val sealed = Envelope.seal(key, "hello".toByteArray(), "x")
        Envelope.open(deriveMasterKey(ByteArray(16) { 8 }), sealed, "x")
    }

    @Test(expected = EnvelopeException::class)
    fun tamperFails() {
        val sealed = Envelope.seal(key, "hello".toByteArray(), "x")
        sealed[sealed.size - 3] = (sealed[sealed.size - 3].toInt() xor 1).toByte()
        Envelope.open(key, sealed, "x")
    }

    @Test(expected = EnvelopeException::class)
    fun swappedObjectNameFails() {
        val sealed = Envelope.seal(key, "hello".toByteArray(), "entries/a.json.enc")
        Envelope.open(key, sealed, "entries/b.json.enc")
    }

    @Test
    fun noncesDiffer() {
        val a = Envelope.seal(key, "same".toByteArray(), "x")
        val b = Envelope.seal(key, "same".toByteArray(), "x")
        assertFalse(a.contentEquals(b))
    }

    @Test
    fun keyCheckVerifies() {
        val check = KeyCheck.create(key)
        assertTrue(KeyCheck.matches(key, check))
        assertFalse(KeyCheck.matches(deriveMasterKey(ByteArray(16) { 9 }), check))
    }
}
