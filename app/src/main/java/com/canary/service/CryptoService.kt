package com.canary.service

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Signature

class CryptoService {

    private val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
    private val secureRandom = SecureRandom()

    fun generateKey(): KeyPair {
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
            return KeyPair(entry.certificate.publicKey, entry.privateKey)
        }

        val generator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            ANDROID_KEY_STORE
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN
        )
            .setAlgorithmParameterSpec(
                java.security.spec.ECGenParameterSpec("secp256r1")
            )
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setKeySize(256)
            .build()

        generator.initialize(spec)
        return generator.generateKeyPair()
    }

    fun getPublicKeyPem(): String {
        val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry
            ?: return ""
        val publicKey = entry.certificate.publicKey
        val encoded = publicKey.encoded
        val b64 = android.util.Base64.encodeToString(encoded, android.util.Base64.NO_WRAP)
        return "-----BEGIN PUBLIC KEY-----\n$b64\n-----END PUBLIC KEY-----"
    }

    fun getPublicKeyFingerprint(): String {
        val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry
            ?: return ""
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(entry.certificate.publicKey.encoded)
        return hash.joinToString("") { "%02x".format(it) }.take(16)
    }

    fun sign(data: ByteArray): ByteArray {
        val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry
            ?: throw IllegalStateException("Key not generated yet")

        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(entry.privateKey)
        signature.update(data)
        return signature.sign()
    }

    fun isKeyGenerated(): Boolean = keyStore.containsAlias(KEY_ALIAS)

    fun generateTagSecret(): ByteArray {
        val secret = ByteArray(32)
        secureRandom.nextBytes(secret)
        return secret
    }

    fun hashSecret(secret: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(secret)
    }

    fun verifyTagSecret(storedHash: ByteArray, providedSecret: ByteArray): Boolean {
        val computedHash = hashSecret(providedSecret)
        return storedHash.contentEquals(computedHash)
    }

    companion object {
        private const val KEY_ALIAS = "canary_ed25519_key"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    }
}
