package com.personal.journal.backup

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class R2Credentials(val accountId: String, val accessKeyId: String, val secretAccessKey: String, val bucket: String)

/** Backup secrets kept on this phone only, each encrypted with a non-exportable Android Keystore key. */
class CredentialStore(context: Context) {
    private val prefs = context.getSharedPreferences("backup_secrets", Context.MODE_PRIVATE)

    private fun localKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return gen.generateKey()
    }

    private fun put(name: String, value: ByteArray) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, localKey()) }
        val sealed = cipher.iv + cipher.doFinal(value)
        prefs.edit().putString(name, Base64.encodeToString(sealed, Base64.NO_WRAP)).apply()
    }

    private fun get(name: String): ByteArray? {
        val sealed = prefs.getString(name, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return null
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, localKey(), GCMParameterSpec(128, sealed, 0, 12))
            cipher.doFinal(sealed, 12, sealed.size - 12)
        }.getOrNull()
    }

    fun saveR2(c: R2Credentials) =
        put("r2", listOf(c.accountId, c.accessKeyId, c.secretAccessKey, c.bucket).joinToString("\n").toByteArray())

    fun loadR2(): R2Credentials? =
        get("r2")?.let { String(it).split("\n") }?.takeIf { it.size == 4 }?.let { R2Credentials(it[0], it[1], it[2], it[3]) }

    fun saveMasterKey(key: SecretKey) = put("master", key.encoded)

    fun loadMasterKey(): SecretKey? = get("master")?.let { SecretKeySpec(it, "AES") }

    fun savePhrase(words: List<String>) = put("phrase", words.joinToString(" ").toByteArray())

    fun loadPhrase(): List<String>? = get("phrase")?.let { String(it).split(" ") }

    fun isConnected(): Boolean = prefs.contains("r2") && prefs.contains("master")

    fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val ALIAS = "journal_local"
    }
}
