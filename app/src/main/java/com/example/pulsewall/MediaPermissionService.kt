package com.example.pulsewall

import android.app.WallpaperManager
import android.content.ComponentName
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.util.Log

class MediaPermissionService : NotificationListenerService() {

    private var lastTrack: String? = null
    private lateinit var manager: MediaSessionManager
    private val handler = Handler(Looper.getMainLooper())
    private var pendingRunnable: Runnable? = null

    override fun onListenerConnected() {
        super.onListenerConnected()

        manager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager

        val component = ComponentName(this, MediaPermissionService::class.java)

        manager.addOnActiveSessionsChangedListener(
                { controllers -> controllers?.let { registerAll(it) } },
                component,
                handler
        )

        registerAll(manager.getActiveSessions(component))
    }

    private fun registerAll(controllers: List<MediaController>) {
        controllers.forEach { controller ->
            controller.registerCallback(
                    object : MediaController.Callback() {

                        override fun onMetadataChanged(metadata: MediaMetadata?) {
                            if (metadata == null) return

                            val title =
                                    metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: return
                            val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""

                            if (title.isBlank()) return

                            val key = "$title-$artist"
                            if (key == lastTrack) return

                            pendingRunnable?.let { handler.removeCallbacks(it) }

                            val runnable = Runnable {
                                val newTitle =
                                        metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
                                                ?: return@Runnable
                                val newArtist =
                                        metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""

                                if (newTitle.isBlank()) return@Runnable

                                val finalKey = "$newTitle-$newArtist"
                                if (finalKey == lastTrack) return@Runnable

                                val art =
                                        metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                                                ?: metadata.getBitmap(
                                                        MediaMetadata.METADATA_KEY_ART
                                                )

                                if (art == null) {
                                    Log.d("PulseWall", "No art for $finalKey")
                                    return@Runnable
                                }

                                lastTrack = finalKey

                                Log.d("PulseWall", finalKey)

                                applyWallpaperSafe(art)
                            }

                            pendingRunnable = runnable
                            handler.postDelayed(runnable, 700)
                        }
                    }
            )
        }
    }

    private fun applyWallpaperSafe(bitmap: Bitmap) {
        try {
            val manager = WallpaperManager.getInstance(this)
            manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
        } catch (e: Exception) {
            Log.e("PulseWall", "Wallpaper failed", e)
        }
    }
}
