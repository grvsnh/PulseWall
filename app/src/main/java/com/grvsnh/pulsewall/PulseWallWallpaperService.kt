package com.grvsnh.pulsewall

import android.app.KeyguardManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

class PulseWallWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        val e = PulseEngine()
        engine = e
        return e
    }

    inner class PulseEngine : Engine() {

        init {
            engine = this
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            redraw()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            redraw()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) redraw()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            if (engine === this) {
                engine = null
            }
        }

        fun redraw() {
            val holder = surfaceHolder
            val canvas =
                    try {
                        holder.lockCanvas()
                    } catch (_: Exception) {
                        null
                    } ?: return

            try {
                drawFrame(canvas)
            } finally {
                try {
                    holder.unlockCanvasAndPost(canvas)
                } catch (_: Exception) {}
            }
        }

        private fun drawFrame(canvas: Canvas) {
            canvas.drawColor(Color.BLACK)

            if (!isLocked()) return

            if (mode == MODE_STOPPED) return

            val art = currentArt ?: return

            val scale =
                    maxOf(
                            canvas.width.toFloat() / art.width.toFloat(),
                            canvas.height.toFloat() / art.height.toFloat()
                    )

            val scaledWidth = art.width * scale
            val scaledHeight = art.height * scale
            val left = (canvas.width - scaledWidth) / 2f
            val top = (canvas.height - scaledHeight) / 2f
            val rect = RectF(left, top, left + scaledWidth, top + scaledHeight)

            val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            paint.alpha = if (mode == MODE_PLAYING) 255 else 140
            canvas.drawBitmap(art, null, rect, paint)

            if (mode == MODE_PAUSED) {
                val dim = Paint()
                dim.color = Color.argb(100, 0, 0, 0)
                canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), dim)
            }
        }

        private fun isLocked(): Boolean {
            val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            return km.isKeyguardLocked
        }
    }

    companion object {
        const val MODE_STOPPED = 0
        const val MODE_PAUSED = 1
        const val MODE_PLAYING = 2

        var currentArt: Bitmap? = null
        var mode: Int = MODE_STOPPED
        var engine: PulseEngine? = null

        fun update(bitmap: Bitmap?, newMode: Int) {
            if (newMode == MODE_STOPPED) {
                currentArt = null
                mode = MODE_STOPPED
            } else {
                if (bitmap != null) currentArt = bitmap
                mode = newMode
            }
            engine?.redraw()
        }

        fun clear() {
            currentArt = null
            mode = MODE_STOPPED
            engine?.redraw()
        }
    }
}
