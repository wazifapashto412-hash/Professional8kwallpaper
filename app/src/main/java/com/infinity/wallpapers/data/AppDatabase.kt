package com.infinity.wallpapers.data

import android.content.Context
import androidx.room.*
import com.infinity.wallpapers.domain.Wallpaper
import kotlinx.coroutines.flow.Flow

/** kind = "fav" | "download" | "recent" */
@Entity(tableName = "saved", primaryKeys = ["kind", "wid"])
data class Saved(
    val kind: String,
    val wid: String,
    val title: String,
    val category: String,
    val thumbUrl: String,
    val previewUrl: String,
    val fullUrl: String,
    val width: Int,
    val height: Int,
    val uri: String?,
    val credit: String?,
    val ts: Long,
)

@Entity(tableName = "searches")
data class SearchEntry(@PrimaryKey val q: String, val ts: Long)

@Dao
interface AppDao {
    @Query("SELECT * FROM saved WHERE kind = :kind ORDER BY ts DESC LIMIT 500")
    fun observe(kind: String): Flow<List<Saved>>

    @Query("SELECT COUNT(*) FROM saved WHERE kind = 'fav' AND wid = :wid")
    fun isFav(wid: String): Flow<Int>

    @Query("SELECT * FROM saved WHERE wid = :wid LIMIT 1")
    suspend fun find(wid: String): Saved?

    @Query("SELECT * FROM saved WHERE kind = 'download' AND wid = :wid LIMIT 1")
    suspend fun download(wid: String): Saved?

    @Query("SELECT * FROM saved WHERE kind = 'recent' ORDER BY ts DESC LIMIT 20")
    suspend fun recentsOnce(): List<Saved>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(s: Saved)

    @Query("DELETE FROM saved WHERE kind = :kind AND wid = :wid")
    suspend fun remove(kind: String, wid: String)

    @Query("DELETE FROM saved WHERE kind = :kind")
    suspend fun clear(kind: String)

    @Query("DELETE FROM saved WHERE kind = 'recent' AND wid NOT IN (SELECT wid FROM saved WHERE kind = 'recent' ORDER BY ts DESC LIMIT 30)")
    suspend fun trimRecents()

    @Query("SELECT q FROM searches ORDER BY ts DESC LIMIT 10")
    fun recentSearches(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putSearch(s: SearchEntry)

    @Query("DELETE FROM searches")
    suspend fun clearSearches()
}

@Database(entities = [Saved::class, SearchEntry::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        fun create(ctx: Context): AppDatabase =
            Room.databaseBuilder(ctx, AppDatabase::class.java, "infinity.db").build()
    }
}

fun Saved.toWallpaper() = Wallpaper(
    id = wid, title = title, category = category,
    thumbUrl = uri ?: thumbUrl, previewUrl = uri ?: previewUrl, fullUrl = fullUrl,
    width = width, height = height, credit = credit, localUri = uri,
)

fun Wallpaper.toSaved(kind: String, uri: String? = null) = Saved(
    kind = kind, wid = id, title = title, category = category,
    thumbUrl = thumbUrl, previewUrl = previewUrl, fullUrl = fullUrl,
    width = width, height = height, uri = uri, credit = credit, ts = System.currentTimeMillis(),
)
