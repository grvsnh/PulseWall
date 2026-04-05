package com.example.pulsewall

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.net.URL

object AlbumArtFetcher {

    fun fetch(title: String, artist: String): Bitmap? {
        return try {
            val query = "$title $artist".replace(" ", "+")
            val url = "https://itunes.apple.com/search?term=$query&entity=song&limit=1"

            val json = URL(url).readText()

            val artworkUrl =
                    Regex("\"artworkUrl100\":\"(.*?)\"")
                            .find(json)
                            ?.groupValues
                            ?.get(1)
                            ?.replace("\\/", "/")
                            ?.replace("100x100", "600x600")

            if (artworkUrl != null) {
                val stream = URL(artworkUrl).openStream()
                BitmapFactory.decodeStream(stream)
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
