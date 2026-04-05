package com.grvsnh.pulsewall

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class PulseWallService : NotificationListenerService() {

    private var lastTrackKey: String? = null
    private var lastMusicPackage: String? = null

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val notification = sbn.notification ?: return
            val extras = notification.extras ?: return

            if (!isMediaNotification(notification, extras)) return

            val title = extras.getString(Notification.EXTRA_TITLE)?.trim().orEmpty()
            val artist = extras.getString(Notification.EXTRA_TEXT)?.trim().orEmpty()
            if (title.isBlank() && artist.isBlank()) return

            val trackKey = "${sbn.packageName}|$title|$artist"
            val mode = resolveMode(notification, extras)
            val bitmap = extractBitmap(notification, extras)

            if (trackKey != lastTrackKey) {
                Log.d("PulseWall", "$title - $artist")
                lastTrackKey = trackKey
                lastMusicPackage = sbn.packageName
            }

            PulseWallWallpaperService.update(bitmap, mode)
        } catch (_: Exception) {}
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName == lastMusicPackage) {
            PulseWallWallpaperService.clear()
            lastTrackKey = null
            lastMusicPackage = null
        }
    }

    private fun isMediaNotification(notification: Notification, extras: Bundle): Boolean {
        return notification.category == Notification.CATEGORY_TRANSPORT ||
                (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0 ||
                extras.containsKey(Notification.EXTRA_MEDIA_SESSION)
    }

    private fun resolveMode(notification: Notification, extras: Bundle): Int {
        val state =
                if (Build.VERSION.SDK_INT >= 33) {
                    extras.getInt("android.mediaPlaybackState", -1)
                } else {
                    @Suppress("DEPRECATION") extras.getInt("android.mediaPlaybackState", -1)
                }

        return when (state) {
            PlaybackState.STATE_PLAYING -> PulseWallWallpaperService.MODE_PLAYING
            PlaybackState.STATE_PAUSED -> PulseWallWallpaperService.MODE_PAUSED
            PlaybackState.STATE_STOPPED, PlaybackState.STATE_NONE ->
                    PulseWallWallpaperService.MODE_STOPPED
            else -> {
                if ((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) {
                    PulseWallWallpaperService.MODE_PLAYING
                } else {
                    PulseWallWallpaperService.MODE_PAUSED
                }
            }
        }
    }

    private fun extractBitmap(notification: Notification, extras: Bundle): Bitmap? {
        when (val raw = extras.get("android.largeIcon")) {
            is Bitmap -> return raw
            is Icon ->
                    iconToBitmap(raw)?.let {
                        return it
                    }
        }

        val direct =
                if (Build.VERSION.SDK_INT >= 33) {
                    extras.getParcelable("android.largeIcon", Bitmap::class.java)
                } else {
                    @Suppress("DEPRECATION") extras.getParcelable("android.largeIcon")
                }

        if (direct is Bitmap) return direct

        val icon = notification.largeIcon
        if (icon != null) {
            iconToBitmap(icon)?.let {
                return it
            }
        }

        return null
    }

    private fun iconToBitmap(icon: Any): Bitmap? {
        return try {
            val method = icon.javaClass.getMethod("loadDrawable", Context::class.java)
            val drawable = method.invoke(icon, this) as? Drawable ?: return null
            drawableToBitmap(drawable)
        } catch (_: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            drawable.bitmap?.let {
                return it
            }
        }

        val width = maxOf(1, drawable.intrinsicWidth)
        val height = maxOf(1, drawable.intrinsicHeight)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
