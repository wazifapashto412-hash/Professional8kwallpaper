package com.infinity.wallpapers.network

import com.infinity.wallpapers.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Pexels API source (free licence, attribution requested). Width/height come from the API,
 * so the 8K / 4K badge reflects the real original resolution.
 * NOTE: a key embedded in an APK can be extracted. For a large public app, proxy this through
 * your own backend (e.g. Cloud Function) - see docs/FIREBASE_SETUP.md.
 */
class PexelsSource(private val key: String) : RemoteSource {
    override val name = "Pexels API"

    private val terms = mapOf(
        "amoled" to "amoled black dark minimal", "nature" to "nature landscape", "mountains" to "mountain peaks",
        "cars" to "car", "supercars" to "supercar sports car", "anime" to "anime illustration art",
        "abstract" to "abstract", "space" to "galaxy space stars", "animals" to "wildlife animals",
        "architecture" to "architecture", "technology" to "technology circuit", "minimalist" to "minimalist",
        "flowers" to "flowers", "luxury" to "luxury", "dark" to "dark moody", "art" to "art photography",
    )

    override suspend fun load(q: WallpaperQuery, cursor: Any?): FeedPage = withContext(Dispatchers.IO) {
        var page = (cursor as? Int) ?: if (q.special == Special.FEATURED) 2 else 1
        val strict = q.special == Special.K8 || q.special == Special.K4
        val min = if (q.special == Special.K8) 7680 else 3840
        val out = mutableListOf<Wallpaper>()
        var end = false
        var attempts = 0
        while (out.size < 12 && attempts < (if (strict) 5 else 1)) {
            val arr = JSONObject(get(url(q, page, if (strict) 80 else 30))).getJSONArray("photos")
            if (arr.length() == 0) { end = true; break }
            for (i in 0 until arr.length()) {
                val w = parse(arr.getJSONObject(i), q)
                if (!strict || w.maxSide >= min) out += w
            }
            page++; attempts++
        }
        FeedPage(out, page, end)
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun url(q: WallpaperQuery, page: Int, per: Int): String {
        val base = "https://api.pexels.com/v1/"
        val tail = "&per_page=$per&page=$page"
        return when {
            q.search != null -> "${base}search?query=${enc(q.search)}&orientation=portrait$tail"
            q.category != null -> "${base}search?query=${enc(terms[q.category] ?: q.category)}&orientation=portrait$tail"
            q.special == Special.K8 || q.special == Special.K4 -> "${base}search?query=${enc("nature wallpaper")}&size=large$tail"
            q.special == Special.TRENDING || q.special == Special.FEATURED -> "${base}curated?per_page=$per&page=$page"
            q.sort == Sort.POPULAR -> "${base}search?query=${enc("popular wallpaper")}&orientation=portrait$tail"
            else -> "${base}search?query=wallpaper&orientation=portrait$tail"
        }
    }

    private fun parse(o: JSONObject, q: WallpaperQuery): Wallpaper {
        val src = o.getJSONObject("src")
        val by = o.optString("photographer", "Pexels")
        val alt = o.optString("alt").trim()
        return Wallpaper(
            id = "px_${o.getLong("id")}",
            title = (alt.ifEmpty { "Wallpaper by $by" }).take(60),
            category = q.category ?: "",
            thumbUrl = src.getString("portrait"),
            previewUrl = src.getString("large2x"),
            fullUrl = src.getString("original"),
            width = o.getInt("width"), height = o.getInt("height"),
            credit = "Photo by $by on Pexels",
        )
    }

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.setRequestProperty("Authorization", key)
            c.connectTimeout = 15_000
            c.readTimeout = 20_000
            if (c.responseCode != 200) throw IOException("HTTP ${c.responseCode}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }
}
