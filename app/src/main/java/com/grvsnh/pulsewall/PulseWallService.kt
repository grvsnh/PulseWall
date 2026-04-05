package com.grvsnh.pulsewall

import android.app.WallpaperManager
import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.util.Log
import java.io.ByteArrayOutputStream

class PulseWallService : NotificationListenerService() {

    private lateinit var mediaSessionManager: MediaSessionManager
    private val handler = Handler(Looper.getMainLooper())

    private var lastTrack: String? = null
    private var lastState: Int = -1

    override fun onListenerConnected() {
        super.onListenerConnected()

        Log.d("PulseWall", "CONNECTED")

        mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        val component = ComponentName(this, PulseWallService::class.java)

        handler.post {
            try {
                val controllers = mediaSessionManager.getActiveSessions(component)
                controllers.forEach { registerController(it) }
            } catch (e: Exception) {
                Log.e("PulseWall", "Session error", e)
            }
        }

        mediaSessionManager.addOnActiveSessionsChangedListener(
                { controllers -> controllers?.forEach { registerController(it) } },
                component
        )
    }

    private fun registerController(controller: MediaController) {

        controller.registerCallback(
                object : MediaController.Callback() {

                    override fun onPlaybackStateChanged(state: PlaybackState?) {
                        if (state == null) return

                        if (state.state == lastState) return
                        lastState = state.state

                        if (state.state == PlaybackState.STATE_PLAYING) {
                            Log.d("PulseWall", "PLAYING")

                            val metadata = controller.metadata
                            if (metadata != null) {
                                val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
                                val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)

                                if (!title.isNullOrBlank()) {
                                    val key = "$title-$artist"
                                    if (key != lastTrack) {
                                        lastTrack = key
                                        Log.d("PulseWall", key)

                                        val art =
                                                metadata.getBitmap(
                                                        MediaMetadata.METADATA_KEY_ALBUM_ART
                                                )
                                                        ?: metadata.getBitmap(
                                                                MediaMetadata.METADATA_KEY_ART
                                                        )

                                        if (art != null) {
                                            applyWallpaper(art)
                                        }
                                    }
                                }
                            }
                        } else if (state.state == PlaybackState.STATE_PAUSED ||
                                        state.state == PlaybackState.STATE_STOPPED
                        ) {
                            Log.d("PulseWall", "PAUSED")
                            lastTrack = null
                        }
                    }
                }
        )
    }

    private fun applyWallpaper(bitmap: Bitmap) {
        try {
            val display = resources.displayMetrics

            val scaled =
                    Bitmap.createScaledBitmap(
                            bitmap,
                            display.widthPixels,
                            display.heightPixels,
                            true
                    )

            val stream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            val bytes = stream.toByteArray()

            val safeBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return

            WallpaperManager.getInstance(this)
                    .setBitmap(safeBitmap, null, true, WallpaperManager.FLAG_LOCK)
        } catch (e: Exception) {
            Log.e("PulseWall", "Wallpaper fail", e)
        }
    }
}
