package com.infinity.wallpapers.presentation

import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.infinity.wallpapers.ServiceLocator
import com.infinity.wallpapers.domain.*
import com.infinity.wallpapers.repository.WallpaperRepository
import com.infinity.wallpapers.utils.friendlyError
import com.infinity.wallpapers.wallpaper.DownloadWorker
import com.infinity.wallpapers.wallpaper.WallTarget
import com.infinity.wallpapers.wallpaper.WallpaperHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** Paginated feed for one query (category, trending, search...). */
class FeedViewModel(private val repo: WallpaperRepository, private val query: WallpaperQuery) : ViewModel() {
    var items by mutableStateOf<List<Wallpaper>>(emptyList()); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var end by mutableStateOf(false); private set
    private var cursor: Any? = null

    init { loadMore() }

    fun loadMore() {
        if (loading || end || error != null) return
        loading = true
        viewModelScope.launch {
            try {
                val p = repo.load(query, cursor)
                cursor = p.next
                end = p.end
                items = (items + p.items).distinctBy { it.id }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = friendlyError(e)
            } finally {
                loading = false
            }
        }
    }

    fun retry() { error = null; loadMore() }
}

class HomeViewModel(private val repo: WallpaperRepository) : ViewModel() {
    data class Section(val title: String, val slug: String?, val items: List<Wallpaper>)

    var sections by mutableStateOf<List<Section>>(emptyList()); private set
    var loading by mutableStateOf(true); private set
    var error by mutableStateOf<String?>(null); private set

    init { load() }

    fun load() {
        loading = true; error = null
        viewModelScope.launch {
            val results = coroutineScope {
                listOf(
                    "Featured" to ("featured" to async { runCatching { repo.load(WallpaperQuery(special = Special.FEATURED), null).items.take(12) } }),
                    "Recommended for you" to (null to async { runCatching { repo.recommended() } }),
                    "Trending now" to ("trending" to async { runCatching { repo.load(WallpaperQuery(special = Special.TRENDING), null).items.take(12) } }),
                    "Popular downloads" to ("popular" to async { runCatching { repo.load(WallpaperQuery(sort = Sort.POPULAR), null).items.take(12) } }),
                    "Latest" to ("latest" to async { runCatching { repo.load(WallpaperQuery(sort = Sort.LATEST), null).items.take(12) } }),
                ).map { (title, p) -> Triple(title, p.first, p.second.await()) }
            }
            val ok = results.filter { it.third.isSuccess && it.third.getOrNull().orEmpty().isNotEmpty() }
            sections = ok.map { Section(it.first, it.second, it.third.getOrThrow()) }
            if (sections.isEmpty()) {
                error = results.firstNotNullOfOrNull { it.third.exceptionOrNull() }?.let { friendlyError(it) }
            }
            loading = false
        }
    }
}

class DetailViewModel(private val repo: WallpaperRepository, private val id: String) : ViewModel() {
    var wallpaper by mutableStateOf<Wallpaper?>(null); private set
    var error by mutableStateOf<String?>(null); private set
    var busy by mutableStateOf(false); private set
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val isFav = repo.isFavorite(id).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init { load() }

    fun load() {
        error = null
        viewModelScope.launch {
            val w = repo.find(id)
            if (w == null) error = "This wallpaper is not available offline." else {
                wallpaper = w
                repo.markViewed(w)
            }
        }
    }

    private fun say(m: String) { messages.tryEmit(m) }

    fun toggleFav() {
        val w = wallpaper ?: return
        viewModelScope.launch {
            val on = !isFav.value
            repo.setFavorite(w, on)
            say(if (on) "Added to favorites" else "Removed from favorites")
        }
    }

    fun setWallpaper(target: WallTarget) {
        val w = wallpaper ?: return
        if (busy) return
        busy = true
        viewModelScope.launch {
            val r = WallpaperHelper.set(ServiceLocator.app, w, target)
            busy = false
            r.onSuccess { say("Wallpaper set on ${target.label}") }
                .onFailure { say("Could not set wallpaper: ${it.message ?: "unknown error"}") }
        }
    }

    fun download() {
        val w = wallpaper ?: return
        if (busy) return
        viewModelScope.launch {
            if (repo.isDownloaded(w.id)) { say("Already in your downloads"); return@launch }
            busy = true
            val wifiOnly = ServiceLocator.settings.wifiOnly.value
            val req = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(workDataOf(
                    "id" to w.id, "url" to w.fullUrl, "title" to w.title, "category" to w.category,
                    "thumb" to w.thumbUrl, "preview" to w.previewUrl, "w" to w.width, "h" to w.height,
                    "credit" to (w.credit ?: ""),
                ))
                .setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED).build())
                .build()
            val wm = WorkManager.getInstance(ServiceLocator.app)
            wm.enqueue(req)
            say(if (wifiOnly) "Download queued - it starts on Wi-Fi" else "Downloading full resolution...")
            val info = wm.getWorkInfoByIdFlow(req.id).first { it?.state?.isFinished == true }!!
            busy = false
            if (info.state == WorkInfo.State.SUCCEEDED) {
                say(if (Build.VERSION.SDK_INT >= 29) "Saved to Pictures/Infinity Wallpapers" else "Saved to the app's Pictures folder")
            } else {
                say(info.outputData.getString("error") ?: "Download failed. Please try again.")
            }
        }
    }

    fun share() {
        val w = wallpaper ?: return
        if (busy) return
        busy = true
        viewModelScope.launch {
            val r = WallpaperHelper.buildShareIntent(ServiceLocator.app, w)
            busy = false
            r.onSuccess { ServiceLocator.app.startActivity(it) }
                .onFailure { say("Could not share: ${it.message ?: "unknown error"}") }
        }
    }
}

class SearchViewModel(private val repo: WallpaperRepository) : ViewModel() {
    val recents = repo.recentSearches().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun submit(q: String) { viewModelScope.launch { repo.saveSearch(q) } }
    fun clear() { viewModelScope.launch { repo.clearSearches() } }
}
