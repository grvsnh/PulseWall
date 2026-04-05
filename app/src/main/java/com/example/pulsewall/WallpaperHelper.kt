package com.example.pulsewall

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap

object WallpaperHelper {

    fun set(context: Context, bitmap: Bitmap) {
        val manager = WallpaperManager.getInstance(context)
        manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
    }
}
