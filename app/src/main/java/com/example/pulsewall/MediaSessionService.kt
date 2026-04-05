package com.example.pulsewall

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Handler
import android.os.Looper
import android.util.Log

class MediaSessionService(private val context: Context) {

    private val manager =
            context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

    private var lastTrack: String? = null

    fun startListening() {
        val handler = Handler(Looper.getMainLooper())

        val component = ComponentName(context, MediaPermissionService::class.java)

        val controllers = manager.getActiveSessions(component)

        controllers.forEach { controller ->
            controller.registerCallback(
                    object : MediaController.Callback() {
                        override fun onMetadataChanged(metadata: MediaMetadata?) {
                            if (metadata == null) return

                            val title =
                                    metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: return
                            val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""

                            val key = "$title-$artist"
                            if (key == lastTrack) return
                            lastTrack = key

                            Log.d("PulseWall", key)

                            val art =
                                    metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                                            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)

                            if (art != null) {
                                WallpaperHelper.set(context, art)
                            }
                        }
                    }
            )
        }
    }
}
