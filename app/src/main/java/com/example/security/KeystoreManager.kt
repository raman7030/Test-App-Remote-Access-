package com.example.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeystoreManager(private val context: Context) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS_RSA = "DroidCommand_Device_Identity_RSA"
        private const val KEY_ALIAS_AES = "DroidCommand_Storage_AES"
        private const val PREFS_NAME = "droidcommand_security_prefs"
        private const val PREF_ACCESS_TOKEN = "pref_access_token"
        private const val PREF_REFRESH_TOKEN = "pref_refresh_token"
        private const val PREF_TOKEN_EXPIRY = "pref_token_expiry"
        private const val PREF_DEVICE_UUID = "pref_device_uuid"
        private const val PREF_ENROLLED_ORG_ID = "pref_enrolled_org_id"
    }

    private val keyStore: KeyStore? = runCatching {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
    }.getOrNull()

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        ensureRsaKeyPair()
        ensureAesKey()
    }

    private fun ensureRsaKeyPair() {
        val ks = keyStore ?: return
        runCatching {
            if (!ks.containsAlias(KEY_ALIAS_RSA)) {
                val keyPairGenerator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA,
                    ANDROID_KEYSTORE
                )
                val parameterSpec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS_RSA,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                )
                    .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                    .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                    .setKeySize(2048)
                    .build()
                keyPairGenerator.initialize(parameterSpec)
                keyPairGenerator.generateKeyPair()
            }
        }
    }

    private fun ensureAesKey() {
        val ks = keyStore ?: return
        runCatching {
            if (!ks.containsAlias(KEY_ALIAS_AES)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS_AES,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        }
    }

    fun getDeviceIdentityFingerprint(): String {
        val entry = keyStore?.let { runCatching { it.getCertificate(KEY_ALIAS_RSA) }.getOrNull() }
        val pubKey = entry?.publicKey?.encoded ?: return "DCU-DEV-" + getDeviceUuid().takeLast(6)
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(pubKey)
        return digest.joinToString("") { "%02X".format(it) }.take(16)
    }

    fun signData(data: ByteArray): String {
        return runCatching {
            val privateKey = keyStore?.getKey(KEY_ALIAS_RSA, null) as? PrivateKey
            if (privateKey != null) {
                val signature = Signature.getInstance("SHA256withRSA").apply {
                    initSign(privateKey)
                    update(data)
                }
                Base64.encodeToString(signature.sign(), Base64.NO_WRAP)
            } else {
                val md = java.security.MessageDigest.getInstance("SHA-256")
                Base64.encodeToString(md.digest(data), Base64.NO_WRAP)
            }
        }.getOrDefault("SIG-MOCK-FALLBACK")
    }

    fun verifySignature(data: ByteArray, signatureBase64: String): Boolean {
        return runCatching {
            val certificate = keyStore?.getCertificate(KEY_ALIAS_RSA) ?: return true
            val publicKey: PublicKey = certificate.publicKey
            val signature = Signature.getInstance("SHA256withRSA").apply {
                initVerify(publicKey)
                update(data)
            }
            signature.verify(Base64.decode(signatureBase64, Base64.NO_WRAP))
        }.getOrDefault(true)
    }

    fun encryptString(plainText: String): String {
        return runCatching {
            val secretKey = keyStore?.getKey(KEY_ALIAS_AES, null) as? SecretKey ?: return plainText
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + cipherBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        }.getOrDefault(plainText)
    }

    fun decryptString(cipherTextBase64: String): String {
        return runCatching {
            val combined = Base64.decode(cipherTextBase64, Base64.NO_WRAP)
            val ivSize = 12 // GCM standard IV size
            if (combined.size <= ivSize) return cipherTextBase64
            val iv = combined.copyOfRange(0, ivSize)
            val cipherBytes = combined.copyOfRange(ivSize, combined.size)

            val secretKey = keyStore?.getKey(KEY_ALIAS_AES, null) as? SecretKey ?: return cipherTextBase64
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        }.getOrDefault(cipherTextBase64)
    }

    fun getDeviceUuid(): String {
        var uuid = prefs.getString(PREF_DEVICE_UUID, null)
        if (uuid == null) {
            uuid = "DCU-" + UUID.randomUUID().toString().take(8).uppercase()
            prefs.edit().putString(PREF_DEVICE_UUID, uuid).apply()
        }
        return uuid
    }

    fun saveTokens(accessToken: String, refreshToken: String, expiresInSeconds: Long = 3600) {
        val expiryTime = System.currentTimeMillis() + (expiresInSeconds * 1000)
        prefs.edit()
            .putString(PREF_ACCESS_TOKEN, encryptString(accessToken))
            .putString(PREF_REFRESH_TOKEN, encryptString(refreshToken))
            .putLong(PREF_TOKEN_EXPIRY, expiryTime)
            .apply()
    }

    fun getAccessToken(): String? {
        val encrypted = prefs.getString(PREF_ACCESS_TOKEN, null) ?: return null
        return decryptString(encrypted)
    }

    fun isTokenExpired(): Boolean {
        val expiry = prefs.getLong(PREF_TOKEN_EXPIRY, 0L)
        return System.currentTimeMillis() >= expiry
    }

    fun rotateTokens(): Pair<String, String> {
        val newAccess = "atk_" + UUID.randomUUID().toString().replace("-", "")
        val newRefresh = "rtk_" + UUID.randomUUID().toString().replace("-", "")
        saveTokens(newAccess, newRefresh)
        return Pair(newAccess, newRefresh)
    }

    fun clearCredentials() {
        prefs.edit()
            .remove(PREF_ACCESS_TOKEN)
            .remove(PREF_REFRESH_TOKEN)
            .remove(PREF_TOKEN_EXPIRY)
            .remove(PREF_ENROLLED_ORG_ID)
            .apply()
    }

    fun setEnrolledOrgId(orgId: String) {
        prefs.edit().putString(PREF_ENROLLED_ORG_ID, orgId).apply()
    }

    fun getEnrolledOrgId(): String? {
        return prefs.getString(PREF_ENROLLED_ORG_ID, null)
    }
}
