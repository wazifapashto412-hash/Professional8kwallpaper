package com.infinity.wallpapers.repository

import com.infinity.wallpapers.data.*
import com.infinity.wallpapers.domain.*
import com.infinity.wallpapers.network.RemoteSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

class WallpaperRepository(private val remote: RemoteSource, db: AppDatabase) {
    private val dao = db.dao()
    private val cache = ConcurrentHashMap<String, Wallpaper>()

    val sourceName: String get() = remote.name

    suspend fun load(q: WallpaperQuery, cursor: Any?): FeedPage {
        val p = remote.load(q, cursor)
        p.items.forEach { cache[it.id] = it }
        return p
    }

    suspend fun remoteCategories(): List<WallCategory> = runCatching { remote.categories() }.getOrDefault(emptyList())

    /** Wallpaper including its local file (if downloaded), so it still opens offline. */
    suspend fun find(id: String): Wallpaper? {
        val base = cache[id] ?: dao.find(id)?.toWallpaper() ?: return null
        val dl = dao.download(id)
        return if (dl != null) base.copy(localUri = dl.uri) else base
    }

    suspend fun isDownloaded(id: String) = dao.download(id) != null

    fun favorites(): Flow<List<Wallpaper>> = dao.observe("fav").map { l -> l.map { it.toWallpaper() } }
    fun downloads(): Flow<List<Wallpaper>> = dao.observe("download").map { l -> l.map { it.toWallpaper() } }
    fun recents(): Flow<List<Wallpaper>> = dao.observe("recent").map { l -> l.take(20).map { it.toWallpaper() } }
    fun isFavorite(id: String): Flow<Boolean> = dao.isFav(id).map { it > 0 }

    suspend fun setFavorite(w: Wallpaper, on: Boolean) {
        if (on) dao.put(w.toSaved("fav")) else dao.remove("fav", w.id)
    }

    suspend fun markViewed(w: Wallpaper) {
        dao.put(w.toSaved("recent"))
        dao.trimRecents()
    }

    suspend fun recommended(): List<Wallpaper> {
        val r = dao.recentsOnce()
        val top = r.groupingBy { it.category }.eachCount().filterKeys { it.isNotBlank() }.maxByOrNull { it.value }?.key
        val q = if (top == null) WallpaperQuery(sort = Sort.POPULAR) else WallpaperQuery(category = top)
        val seen = r.map { it.wid }.toSet()
        return load(q, null).items.filter { it.id !in seen }.take(12)
    }

    fun recentSearches(): Flow<List<String>> = dao.recentSearches()
    suspend fun saveSearch(q: String) { if (q.isNotBlank()) dao.putSearch(SearchEntry(q.trim(), System.currentTimeMillis())) }
    suspend fun clearSearches() = dao.clearSearches()
    suspend fun clearRecents() = dao.clear("recent")
    suspend fun clearDownloadHistory() = dao.clear("download")
}
