package com.example.security

import android.util.Base64
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

object CryptoUtils {
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128 // 128-bit authentication tag

    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        return try {
            val secretKey = KeyStoreManager.getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            
            // Combine IV and Ciphertext into one byte array
            val combinedBytes = ByteBuffer.allocate(iv.size + encryptedBytes.size)
                .put(iv)
                .put(encryptedBytes)
                .array()
                
            Base64.encodeToString(combinedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            throw DecryptionException("Gagal mengenkripsi data: ${e.localizedMessage}", e)
        }
    }

    fun decrypt(encryptedBase64: String): String {
        if (encryptedBase64.isEmpty()) return ""
        return try {
            val secretKey = KeyStoreManager.getOrCreateSecretKey()
            val combinedBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            
            if (combinedBytes.size <= GCM_IV_LENGTH) {
                throw DecryptionException("Data terenkripsi tidak valid atau rusak (terlalu pendek).")
            }
            
            val buffer = ByteBuffer.wrap(combinedBytes)
            val iv = ByteArray(GCM_IV_LENGTH)
            buffer.get(iv)
            
            val ciphertext = ByteArray(buffer.remaining())
            buffer.get(ciphertext)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            
            val decryptedBytes = cipher.doFinal(ciphertext)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            throw DecryptionException("Gagal mendekripsi data: kemungkinan kunci telah berubah atau data rusak.", e)
        }
    }
}
