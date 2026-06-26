package com.example.utils

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object Cryptographer {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    
    // A secure 16-byte fixed key for draft backup encryption
    // In production, this can be retrieved dynamically from the Android KeyStore
    private val keyBytes = byteArrayOf(
        0x4D, 0x65, 0x79, 0x64, 0x69, 0x41, 0x49, 0x53,
        0x65, 0x63, 0x75, 0x72, 0x65, 0x4B, 0x65, 0x79 // "MeydiAISecureKey"
    )
    
    private val ivBytes = byteArrayOf(
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
        0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F
    )

    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        return try {
            val keySpec = SecretKeySpec(keyBytes, "AES")
            val ivSpec = IvParameterSpec(ivBytes)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            plainText // Fallback to plain text on encryption error
        }
    }

    fun decrypt(encryptedText: String): String {
        if (encryptedText.isEmpty()) return ""
        return try {
            val keySpec = SecretKeySpec(keyBytes, "AES")
            val ivSpec = IvParameterSpec(ivBytes)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            val decoded = Base64.decode(encryptedText, Base64.NO_WRAP)
            val decrypted = cipher.doFinal(decoded)
            String(decrypted, Charsets.UTF_8)
        } catch (e: java.lang.IllegalArgumentException) {
            // Not base64, likely raw text before encryption was enabled
            encryptedText
        } catch (e: Exception) {
            e.printStackTrace()
            encryptedText // Fallback to original
        }
    }
}
