package com.infinity.wallpapers.network

import com.infinity.wallpapers.domain.*
import kotlinx.coroutines.delay

/**
 * Offline-safe demo catalogue used ONLY when neither Firebase nor a Pexels key is configured.
 * Images come from Picsum (Unsplash-licensed photos). They are requested at 2160x3840 and are NOT
 * genuine 8K, so they are never labelled 8K. Configure Firebase or Pexels for real 8K content.
 */
class DemoSource : RemoteSource {
    override val name = "Demo"

    private val all: List<Wallpaper> by lazy {
        val cats = Categories.defaults.filter { it.query.category != null }
        val now = System.currentTimeMillis()
        List(160) { i ->
            val c = cats[i % cats.size]
            val seed = "infinity-${c.slug}-$i"
            val base = "https://picsum.photos/seed/$seed"
            Wallpaper(
                id = "demo_$i",
                title = "${c.name.removeSuffix(" Wallpapers")} ${i + 1}",
                category = c.slug,
                thumbUrl = "$base/400/700",
                previewUrl = "$base/1080/1920",
                fullUrl = "$base/2160/3840",
                width = 2160, height = 3840,
                trending = i % 5 == 0,
                featured = i % 7 == 0,
                downloads = ((i * 7919L) % 5000),
                createdAt = now - i * 3_600_000L,
                credit = "Demo image via Picsum (Unsplash licence)",
            )
        }
    }

    override suspend fun load(q: WallpaperQuery, cursor: Any?): FeedPage {
        delay(250)
        var list = all
        q.category?.let { c -> list = list.filter { it.category == c } }
        list = when (q.special) {
            Special.TRENDING -> list.filter { it.trending }
            Special.FEATURED -> list.filter { it.featured }
            Special.K8 -> list.filter { it.maxSide >= 7680 }
            Special.K4 -> list.filter { it.maxSide >= 3840 }
            Special.NONE -> list
        }
        q.search?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }?.let { t ->
            list = list.filter { w ->
                w.title.lowercase().contains(t) || w.category.contains(t) ||
                    Categories.defaults.any { it.slug == w.category && it.name.lowercase().contains(t) }
            }
        }
        list = when (q.sort) {
            Sort.LATEST -> list.sortedByDescending { it.createdAt }
            Sort.TRENDING -> list.sortedByDescending { it.trending }
            Sort.POPULAR -> list.sortedByDescending { it.downloads }
        }
        val start = (cursor as? Int) ?: 0
        val items = list.drop(start).take(PAGE_SIZE)
        val next = start + items.size
        return FeedPage(items, next, next >= list.size)
    }
}
