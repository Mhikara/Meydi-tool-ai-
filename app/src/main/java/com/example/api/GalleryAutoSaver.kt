package com.example.api

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Utilitas untuk mengelola penyimpanan gambar secara aman ke dalam galeri lokal.
 * Dirancang untuk berjalan di background thread (I/O) guna mencegah ANR (Aplikasi Crash)
 * dan mengelola alokasi memori bitmap.
 */
object GalleryAutoSaver {

    sealed class SaveResult {
        data class Success(val uri: Uri, val message: String) : SaveResult()
        data class Error(val exception: Exception, val message: String) : SaveResult()
    }

    /**
     * Menyimpan gambar bitmap ke penyimpanan lokal menggunakan pendekatan modern (Scoped Storage).
     *
     * @param context Konteks aplikasi
     * @param bitmap Gambar yang akan disimpan (Akan diabaikan jika recycle() dipanggil setelahnya)
     * @param filename Nama file tanpa ekstensi gambar
     */
    suspend fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        filename: String,
        folderName: String = "MeydiGallery"
    ): SaveResult {
        // Pindahkan beban kerja berat kompresi ke Dispatcher I/O agar UI tidak nge-freeze
        return withContext(Dispatchers.IO) {
            try {
                var imageOutStream: OutputStream? = null
                var imageUri: Uri? = null

                // Modern Android (API 29+ / Android 10+): Menggunakan MediaStore Scoped Storage
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "$filename.jpg")
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        // Konfigurasi path relatif agar tidak butuh izin write external storage strict
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/$folderName")
                        put(MediaStore.Images.Media.IS_PENDING, 1) // Kunci file selama buffering
                    }

                    val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    imageUri = context.contentResolver.insert(collection, values)

                    if (imageUri != null) {
                        imageOutStream = context.contentResolver.openOutputStream(imageUri)
                    } else {
                        return@withContext SaveResult.Error(Exception("Gagal membuat record di MediaStore."), "Gagal menyimpan ke memori.")
                    }

                    // Tulis kompresi ke stream
                    imageOutStream?.use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream) // Kualitas 90 untuk cegah OutOfMemory
                    }

                    // Buka kunci (Release pending)
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(imageUri, values, null, null)

                } else {
                    // Legacy Android (Di bawah API 29)
                    val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
                    val targetDir = File(imagesDir, folderName)
                    
                    if (!targetDir.exists()) {
                        targetDir.mkdirs()
                    }
                    
                    val imageFile = File(targetDir, "$filename.jpg")
                    imageOutStream = FileOutputStream(imageFile)
                    
                    imageOutStream.use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                    }
                    imageUri = Uri.fromFile(imageFile)
                }

                SaveResult.Success(imageUri!!, "Gambar berhasil di-autosave ke galeri.")
            } catch (e: Exception) {
                e.printStackTrace()
                SaveResult.Error(e, "Terjadi kesalahan saat aut-save: ${e.message}")
            } 
            // Note: Caller yang bertanggung jawab untuk memanggil `bitmap.recycle()`
            // setelah save selesai agar terhindar dari Memory Leak, bukan kelas Utilitas ini.
        }
    }
}
