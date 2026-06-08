package com.canary.service

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.common.BitMatrix
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class QrService {

    private val secureRandom = SecureRandom()

    fun generatePassphrase(): String {
        val bytes = ByteArray(4)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun encryptBackupData(data: ByteArray, passphrase: String): ByteArray {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        val key = deriveKey(passphrase)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data)

        return iv + encrypted
    }

    fun decryptBackupData(encryptedData: ByteArray, passphrase: String): ByteArray? {
        return try {
            val key = deriveKey(passphrase)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = encryptedData.copyOfRange(0, 12)
            val ciphertext = encryptedData.copyOfRange(12, encryptedData.size)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            cipher.doFinal(ciphertext)
        } catch (_: Exception) {
            null
        }
    }

    fun generateQrBitmap(
        data: String,
        size: Int = 512,
    ): Bitmap {
        val writer = QRCodeWriter()
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val bitMatrix: BitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, size, size, hints)

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }
        return bitmap
    }

    private fun deriveKey(passphrase: String): SecretKey {
        val keyBytes = passphrase.toByteArray(StandardCharsets.UTF_8)
            .copyOf(32) // pad or truncate to 32 bytes
        return javax.crypto.spec.SecretKeySpec(keyBytes, "AES")
    }
}
