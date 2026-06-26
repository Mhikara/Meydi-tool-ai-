package com.example.autoclipper.processing

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageProcessor {

    suspend fun cropBitmap(original: Bitmap, rect: Rect): Bitmap? {
        return withContext(Dispatchers.Default) {
            try {
                // Ensure rect is within bitmap bounds
                val safeLeft = maxOf(0, rect.left)
                val safeTop = maxOf(0, rect.top)
                val safeRight = minOf(original.width, rect.right)
                val safeBottom = minOf(original.height, rect.bottom)
                
                val width = safeRight - safeLeft
                val height = safeBottom - safeTop
                
                if (width <= 0 || height <= 0) return@withContext null
                
                Bitmap.createBitmap(original, safeLeft, safeTop, width, height)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    
    suspend fun scaleBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        return withContext(Dispatchers.Default) {
            val width = bitmap.width
            val height = bitmap.height
            
            if (width <= maxWidth && height <= maxHeight) {
                return@withContext bitmap
            }
            
            val ratioBitmap = width.toFloat() / height.toFloat()
            val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()

            var finalWidth = maxWidth
            var finalHeight = maxHeight
            if (ratioMax > ratioBitmap) {
                finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
            } else {
                finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
            }
            
            Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
        }
    }
}
