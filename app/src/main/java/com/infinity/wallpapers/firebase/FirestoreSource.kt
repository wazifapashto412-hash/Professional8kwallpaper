package com.infinity.wallpapers.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.infinity.wallpapers.domain.*
import com.infinity.wallpapers.network.PAGE_SIZE
import com.infinity.wallpapers.network.RemoteSource
import kotlinx.coroutines.tasks.await

/**
 * Collections:
 *  wallpapers/{id}: title, category(slug), thumbUrl, previewUrl, fullUrl, width, height, maxSide,
 *                   trending, featured, downloads, createdAt(millis or Timestamp), credit
 *  categories/{slug}: name, order (optional) - adds extra categories remotely
 * Some filter+sort combinations need a composite index: Firestore logs a link to create it.
 */
class FirestoreSource : RemoteSource {
    override val name = "Firebase Firestore"
    private val db get() = FirebaseFirestore.getInstance()

    override suspend fun load(q: WallpaperQuery, cursor: Any?): FeedPage {
        if (q.search != null) return search(q.search)
        var ref: Query = db.collection("wallpapers")
        q.category?.let { ref = ref.whereEqualTo("category", it) }
        ref = when (q.special) {
            Special.TRENDING -> ref.whereEqualTo("trending", true)
            Special.FEATURED -> ref.whereEqualTo("featured", true)
            Special.K8 -> ref.whereGreaterThanOrEqualTo("maxSide", 7680)
            Special.K4 -> ref.whereGreaterThanOrEqualTo("maxSide", 3840)
            Special.NONE -> ref
        }
        ref = when {
            q.special == Special.K8 || q.special == Special.K4 -> ref.orderBy("maxSide", Query.Direction.DESCENDING)
            q.sort == Sort.POPULAR -> ref.orderBy("downloads", Query.Direction.DESCENDING)
            else -> ref.orderBy("createdAt", Query.Direction.DESCENDING)
        }
        (cursor as? DocumentSnapshot)?.let { ref = ref.startAfter(it) }
        val snap = ref.limit(PAGE_SIZE.toLong()).get().await()
        val items = snap.documents.mapNotNull { it.toWallpaper() }
        return FeedPage(items, snap.documents.lastOrNull(), snap.size() < PAGE_SIZE)
    }

    // Client-side search over the newest 300 wallpapers (simple, no extra service needed).
    private suspend fun search(text: String): FeedPage {
        val t = text.trim().lowercase()
        val snap = db.collection("wallpapers").orderBy("createdAt", Query.Direction.DESCENDING).limit(300).get().await()
        val items = snap.documents.mapNotNull { it.toWallpaper() }.filter {
            it.title.lowercase().contains(t) || it.category.lowercase().contains(t)
        }
        return FeedPage(items, null, true)
    }

    override suspend fun categories(): List<WallCategory> {
        val snap = db.collection("categories").get().await()
        return snap.documents
            .sortedBy { it.getLong("order") ?: 1000L }
            .mapNotNull { d ->
                val n = d.getString("name") ?: return@mapNotNull null
                WallCategory(d.id, n, WallpaperQuery(category = d.id))
            }
    }

    private fun DocumentSnapshot.toWallpaper(): Wallpaper? {
        val full = getString("fullUrl") ?: return null
        val created = when (val c = get("createdAt")) {
            is Timestamp -> c.toDate().time
            is Number -> c.toLong()
            else -> 0L
        }
        return Wallpaper(
            id = id,
            title = getString("title") ?: "Wallpaper",
            category = getString("category") ?: "",
            thumbUrl = getString("thumbUrl") ?: full,
            previewUrl = getString("previewUrl") ?: full,
            fullUrl = full,
            width = (getLong("width") ?: 0L).toInt(),
            height = (getLong("height") ?: 0L).toInt(),
            trending = getBoolean("trending") == true,
            featured = getBoolean("featured") == true,
            downloads = getLong("downloads") ?: 0L,
            createdAt = created,
            credit = getString("credit"),
        )
    }
}
