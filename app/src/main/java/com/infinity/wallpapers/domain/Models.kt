package com.infinity.wallpapers.domain

data class Wallpaper(
    val id: String,
    val title: String,
    val category: String,
    val thumbUrl: String,
    val previewUrl: String,
    val fullUrl: String,
    val width: Int,
    val height: Int,
    val trending: Boolean = false,
    val featured: Boolean = false,
    val downloads: Long = 0,
    val createdAt: Long = 0,
    val credit: String? = null,
    val localUri: String? = null,
) {
    val maxSide: Int get() = maxOf(width, height)
    val resolutionLabel: String get() = if (width > 0 && height > 0) "$width × $height px" else "Unknown"

    /** Badge is derived ONLY from the real pixel size of the original file. */
    val badge: String?
        get() = when {
            maxSide >= 7680 -> "8K"
            maxSide >= 3840 -> "4K"
            else -> null
        }
}

enum class Sort { LATEST, TRENDING, POPULAR }
enum class Special { NONE, TRENDING, FEATURED, K8, K4 }

data class WallpaperQuery(
    val category: String? = null,
    val special: Special = Special.NONE,
    val sort: Sort = Sort.LATEST,
    val search: String? = null,
)

data class FeedPage(val items: List<Wallpaper>, val next: Any?, val end: Boolean)

data class WallCategory(val slug: String, val name: String, val query: WallpaperQuery)

object Categories {
    private fun cat(slug: String, name: String) = WallCategory(slug, name, WallpaperQuery(category = slug))

    val defaults: List<WallCategory> = listOf(
        WallCategory("trending", "Trending Wallpapers", WallpaperQuery(special = Special.TRENDING)),
        WallCategory("8k", "8K Ultra HD", WallpaperQuery(special = Special.K8, sort = Sort.POPULAR)),
        WallCategory("4k", "4K Wallpapers", WallpaperQuery(special = Special.K4, sort = Sort.POPULAR)),
        cat("amoled", "AMOLED Wallpapers"),
        cat("nature", "Nature"),
        cat("mountains", "Mountains"),
        cat("cars", "Cars"),
        cat("supercars", "Supercars"),
        cat("anime", "Anime"),
        cat("abstract", "Abstract"),
        cat("space", "Space and Galaxy"),
        cat("animals", "Animals"),
        cat("architecture", "Architecture"),
        cat("technology", "Technology"),
        cat("minimalist", "Minimalist"),
        cat("flowers", "Flowers"),
        cat("luxury", "Luxury"),
        cat("dark", "Dark Wallpapers"),
        cat("art", "Art and Photography"),
        WallCategory("latest", "Latest Wallpapers", WallpaperQuery(sort = Sort.LATEST)),
    )

    private val hidden = listOf(
        WallCategory("popular", "Popular Downloads", WallpaperQuery(sort = Sort.POPULAR)),
        WallCategory("featured", "Featured", WallpaperQuery(special = Special.FEATURED)),
    )

    /** Resolves default, hidden and remote (Firestore-defined) categories. */
    fun resolve(slug: String): WallCategory =
        (defaults + hidden).firstOrNull { it.slug == slug }
            ?: WallCategory(slug, slug.replaceFirstChar { it.uppercase() }, WallpaperQuery(category = slug))
}
