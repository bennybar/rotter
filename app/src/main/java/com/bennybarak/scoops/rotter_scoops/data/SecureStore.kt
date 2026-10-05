package com.bennybarak.scoops.rotter_scoops.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.math.BigInteger
import java.security.Key
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.util.Calendar
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.security.auth.x500.X500Principal

/**
 * Credentials (rotter username + password, the OpenAI key), kept in the
 * Keystore-backed format the Flutter build's `flutter_secure_storage` (v9,
 * default options) used: an AES key wrapped by an RSA Keystore key, values
 * AES/CBC-encrypted with a random IV and stored base64 under a prefixed key.
 * Reading that format means an update keeps the user signed in.
 *
 * Call from a background thread: Keystore operations block.
 */
object SecureStore {
    private const val TAG = "SecureStore"
    private const val PREFS = "FlutterSecureStorage"
    private const val KEY_PREFS = "FlutterSecureKeyStorage"
    private const val ELEMENT_PREFIX = "VGhpcyBpcyB0aGUgcHJlZml4IGZvciBhIHNlY3VyZSBzdG9yYWdlCg"
    private const val AES_PREF_KEY = "VGhpcyBpcyB0aGUga2V5IGZvciBhIHNlY3VyZSBzdG9yYWdlIEFFUyBLZXkK"
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"

    private lateinit var context: Context
    private var secretKey: Key? = null
    private val random = SecureRandom()

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    @Synchronized
    fun read(key: String): String? {
        val raw = prefs().getString(ELEMENT_PREFIX + "_" + key, null) ?: return null
        return try {
            val data = Base64.decode(raw, 0)
            val iv = data.copyOfRange(0, 16)
            val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
            cipher.init(Cipher.DECRYPT_MODE, aesKey(), IvParameterSpec(iv))
            String(cipher.doFinal(data, 16, data.size - 16), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "read failed", e)
            null
        }
    }

    @Synchronized
    fun write(key: String, value: String) {
        try {
            val iv = ByteArray(16).also(random::nextBytes)
            val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
            cipher.init(Cipher.ENCRYPT_MODE, aesKey(), IvParameterSpec(iv))
            val payload = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            prefs().edit()
                .putString(ELEMENT_PREFIX + "_" + key, Base64.encodeToString(iv + payload, 0))
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "write failed", e)
        }
    }

    @Synchronized
    fun delete(key: String) {
        prefs().edit().remove(ELEMENT_PREFIX + "_" + key).apply()
    }

    private fun prefs() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun aesKey(): Key {
        secretKey?.let { return it }
        val keyPrefs = context.getSharedPreferences(KEY_PREFS, Context.MODE_PRIVATE)
        val alias = context.packageName + ".FlutterSecureStoragePluginKey"
        ensureRsaKey(alias)
        keyPrefs.getString(AES_PREF_KEY, null)?.let { wrapped ->
            try {
                val rsa = rsaCipher()
                rsa.init(Cipher.UNWRAP_MODE, privateKey(alias))
                return rsa.unwrap(Base64.decode(wrapped, Base64.DEFAULT), "AES", Cipher.SECRET_KEY)
                    .also { secretKey = it }
            } catch (e: Exception) {
                Log.e(TAG, "unwrap key failed", e)
            }
        }
        val fresh = SecretKeySpec(ByteArray(16).also(random::nextBytes), "AES")
        val rsa = rsaCipher()
        val ks = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        rsa.init(Cipher.WRAP_MODE, ks.getCertificate(alias).publicKey)
        keyPrefs.edit()
            .putString(AES_PREF_KEY, Base64.encodeToString(rsa.wrap(fresh), Base64.DEFAULT))
            .apply()
        secretKey = fresh
        return fresh
    }

    private fun rsaCipher(): Cipher =
        Cipher.getInstance("RSA/ECB/PKCS1Padding", "AndroidKeyStoreBCWorkaround")

    private fun privateKey(alias: String): PrivateKey {
        val ks = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        return ks.getKey(alias, null) as PrivateKey
    }

    private fun ensureRsaKey(alias: String) {
        val ks = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        if (ks.getKey(alias, null) != null) return
        // Key generation formats dates with the default locale, which breaks
        // under Hebrew/Arabic locales; the plugin forced English around it too.
        val before = Locale.getDefault()
        try {
            Locale.setDefault(Locale.ENGLISH)
            val start = Calendar.getInstance()
            val end = Calendar.getInstance().apply { add(Calendar.YEAR, 25) }
            val spec = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_ENCRYPT,
            )
                .setCertificateSubject(X500Principal("CN=$alias"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                .setCertificateSerialNumber(BigInteger.valueOf(1))
                .setCertificateNotBefore(start.time)
                .setCertificateNotAfter(end.time)
                .build()
            KeyPairGenerator.getInstance("RSA", ANDROID_KEY_STORE).apply {
                initialize(spec)
                generateKeyPair()
            }
        } finally {
            Locale.setDefault(before)
        }
    }
}
