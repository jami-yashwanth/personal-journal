package com.personal.journal.backup

import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class InvalidPhraseException(message: String) : IllegalArgumentException(message)

class EnvelopeException(cause: Throwable? = null) : Exception("Backup data couldn't be decrypted", cause)

/** 12-word BIP-39 phrase over 128 bits of entropy. */
class RecoveryPhrase(private val words: List<String>) {
    private val index = words.withIndex().associate { (i, w) -> w to i }

    init {
        require(words.size == 2048) { "BIP-39 word list must have 2048 words" }
    }

    fun generate(random: SecureRandom): List<String> = fromEntropy(ByteArray(16).also(random::nextBytes))

    fun fromEntropy(entropy: ByteArray): List<String> {
        require(entropy.size == 16)
        val checksum = MessageDigest.getInstance("SHA-256").digest(entropy)[0].toInt() and 0xFF
        // 128 entropy bits + top 4 checksum bits = 132 bits = 12 x 11-bit indices.
        val bits = entropy.joinToString("") { (it.toInt() and 0xFF).toString(2).padStart(8, '0') } +
            checksum.toString(2).padStart(8, '0').take(4)
        return bits.chunked(11).map { words[it.toInt(2)] }
    }

    fun toEntropy(phrase: List<String>): ByteArray {
        if (phrase.size != 12) throw InvalidPhraseException("The recovery phrase has 12 words")
        val bits = phrase.joinToString("") { w ->
            val i = index[w] ?: throw InvalidPhraseException("\"$w\" isn't a recovery phrase word")
            i.toString(2).padStart(11, '0')
        }
        val entropy = bits.take(128).chunked(8).map { it.toInt(2).toByte() }.toByteArray()
        if (fromEntropy(entropy) != phrase) throw InvalidPhraseException("Check the words and their order")
        return entropy
    }

    /** Accepts any spacing, line breaks and capitalisation. */
    fun parse(input: String): List<String> = input.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
}

fun hkdfSha256(ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(if (salt.isEmpty()) ByteArray(32) else salt, "HmacSHA256"))
    val prk = mac.doFinal(ikm)
    mac.init(SecretKeySpec(prk, "HmacSHA256"))
    val out = ByteArray(length)
    var previous = ByteArray(0)
    var offset = 0
    var counter = 1
    while (offset < length) {
        mac.update(previous)
        mac.update(info)
        mac.update(counter.toByte())
        previous = mac.doFinal()
        val n = minOf(previous.size, length - offset)
        System.arraycopy(previous, 0, out, offset, n)
        offset += n
        counter++
    }
    return out
}

fun deriveMasterKey(entropy: ByteArray): SecretKey =
    SecretKeySpec(hkdfSha256(entropy, ByteArray(0), "journal-backup-v1".toByteArray(), 32), "AES")

/** version(1) | nonce(12) | AES-256-GCM ciphertext+tag. The object name is bound as associated data. */
object Envelope {
    const val VERSION: Byte = 1
    private const val NONCE = 12
    private val random = SecureRandom()

    fun seal(key: SecretKey, plain: ByteArray, objectName: String): ByteArray {
        val nonce = ByteArray(NONCE).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, nonce))
        cipher.updateAAD(objectName.toByteArray())
        return byteArrayOf(VERSION) + nonce + cipher.doFinal(plain)
    }

    fun open(key: SecretKey, sealed: ByteArray, objectName: String): ByteArray {
        if (sealed.size < 1 + NONCE + 16 || sealed[0] != VERSION) throw EnvelopeException()
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, sealed, 1, NONCE))
            cipher.updateAAD(objectName.toByteArray())
            cipher.doFinal(sealed, 1 + NONCE, sealed.size - 1 - NONCE)
        } catch (e: GeneralSecurityException) {
            throw EnvelopeException(e)
        }
    }
}

/** Lets restore check a recovery phrase before downloading anything. */
object KeyCheck {
    private const val NAME = "meta.json.enc"
    private val PLAIN = "journal-key-check".toByteArray()

    fun create(key: SecretKey): ByteArray = Envelope.seal(key, PLAIN, NAME)

    fun matches(key: SecretKey, sealed: ByteArray): Boolean =
        runCatching { Envelope.open(key, sealed, NAME).contentEquals(PLAIN) }.getOrDefault(false)
}
