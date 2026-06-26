package com.example.autoclipper.ai

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.delay

class SegmentationEngine {
    
    /**
     * MOCK: Removes the background from a given bitmap.
     */
    suspend fun removeBackground(bitmap: Bitmap): Bitmap? {
        // Simulasi delay ML processing
        delay(1500)
        
        // Buat bitmap transparan dengan border simulasi crop object
        val resultBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(resultBitmap)
        
        // Mock: Hapus pixel sekeliling, tinggalkan area tengah saja
        val paint = android.graphics.Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
        }
        
        val centerX = bitmap.width / 2f
        val centerY = bitmap.height / 2f
        val radius = minOf(centerX, centerY) * 0.8f
        
        // Buat mask oval di tengah
        val maskBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val maskCanvas = android.graphics.Canvas(maskBitmap)
        maskCanvas.drawCircle(centerX, centerY, radius, paint)
        
        // Potong original bitmap menggunakan mask
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(maskBitmap, 0f, 0f, null)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return resultBitmap
    }
    
    suspend fun replaceBackgroundWithColor(bitmap: Bitmap, color: Int): Bitmap? {
        val foreground = removeBackground(bitmap) ?: return null
        val resultBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(resultBitmap)
        canvas.drawColor(color)
        canvas.drawBitmap(foreground, 0f, 0f, null)
        return resultBitmap
    }
}
