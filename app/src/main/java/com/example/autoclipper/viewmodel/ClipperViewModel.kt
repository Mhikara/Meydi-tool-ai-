package com.example.autoclipper.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.autoclipper.ai.DetectedObjectResult
import com.example.autoclipper.ai.ObjectDetectionEngine
import com.example.autoclipper.ai.SegmentationEngine
import com.example.autoclipper.processing.ImageProcessor
import com.example.autoclipper.storage.ClipperStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ClipperViewModel(application: Application) : AndroidViewModel(application) {

    private val segmentationEngine = SegmentationEngine()
    private val objectDetectionEngine = ObjectDetectionEngine()
    private val imageProcessor = ImageProcessor()
    private val storageManager = ClipperStorageManager(application)

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private val _originalBitmap = MutableStateFlow<Bitmap?>(null)
    val originalBitmap: StateFlow<Bitmap?> = _originalBitmap

    private val _processedBitmap = MutableStateFlow<Bitmap?>(null)
    val processedBitmap: StateFlow<Bitmap?> = _processedBitmap

    private val _detectedObjects = MutableStateFlow<List<DetectedObjectResult>>(emptyList())
    val detectedObjects: StateFlow<List<DetectedObjectResult>> = _detectedObjects

    private val _processMessage = MutableStateFlow("")
    val processMessage: StateFlow<String> = _processMessage

    fun loadImage(uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processMessage.value = "Memuat gambar..."
            try {
                val context = getApplication<Application>()
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                
                // Scale down if too large for ML Kit
                val scaledBitmap = imageProcessor.scaleBitmap(bitmap, 1024, 1024)
                
                _originalBitmap.value = scaledBitmap
                _processedBitmap.value = scaledBitmap
                _detectedObjects.value = emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                _processMessage.value = "Gagal memuat gambar"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun removeBackground() {
        val bitmap = _originalBitmap.value ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            _processMessage.value = "Menghapus background..."
            val result = segmentationEngine.removeBackground(bitmap)
            if (result != null) {
                _processedBitmap.value = result
                _processMessage.value = "Background berhasil dihapus!"
            } else {
                _processMessage.value = "Gagal menghapus background"
            }
            _isProcessing.value = false
        }
    }

    fun detectObjects() {
        val bitmap = _originalBitmap.value ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            _processMessage.value = "Mendeteksi objek..."
            val objects = objectDetectionEngine.detectObjects(bitmap)
            _detectedObjects.value = objects
            _processMessage.value = "Ditemukan ${objects.size} objek"
            _isProcessing.value = false
        }
    }

    fun cropObject(detectedObject: DetectedObjectResult) {
        val bitmap = _originalBitmap.value ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            _processMessage.value = "Memotong objek..."
            val cropped = imageProcessor.cropBitmap(bitmap, detectedObject.boundingBox)
            if (cropped != null) {
                _processedBitmap.value = cropped
                _processMessage.value = "Objek berhasil dipotong!"
            } else {
                _processMessage.value = "Gagal memotong objek"
            }
            _isProcessing.value = false
        }
    }

    fun saveResult(onComplete: (Boolean) -> Unit) {
        val bitmap = _processedBitmap.value ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            _processMessage.value = "Menyimpan gambar..."
            val filename = "AutoClipper_${System.currentTimeMillis()}"
            val success = storageManager.saveBitmapToGallery(bitmap, filename)
            _isProcessing.value = false
            _processMessage.value = if (success) "Disimpan ke Galeri!" else "Gagal menyimpan"
            onComplete(success)
        }
    }
    
    fun resetEdit() {
        _processedBitmap.value = _originalBitmap.value
        _processMessage.value = "Edit direset"
    }
}
