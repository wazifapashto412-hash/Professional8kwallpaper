package com.infinity.wallpapers.wallpaper

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.infinity.wallpapers.ServiceLocator
import com.infinity.wallpapers.data.Saved
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Saves the full-resolution file. Android 10+: MediaStore (Pictures/Infinity Wallpapers) - no storage permission.
 * Android 8-9: app-specific external folder - also no permission needed.
 */
class DownloadWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val d = inputData
        val id = d.getString("id") ?: return Result.failure()
        val url = d.getString("url") ?: return Result.failure()
        return try {
            val uri = withContext(Dispatchers.IO) { save(url, id) }
            ServiceLocator.db.dao().put(
                Saved(
                    kind = "download", wid = id,
                    title = d.getString("title") ?: "Wallpaper",
                    category = d.getString("category") ?: "",
                    thumbUrl = d.getString("thumb") ?: url,
                    previewUrl = d.getString("preview") ?: url,
                    fullUrl = url,
                    width = d.getInt("w", 0), height = d.getInt("h", 0),
                    uri = uri,
                    credit = d.getString("credit")?.takeIf { it.isNotBlank() },
                    ts = System.currentTimeMillis(),
                )
            )
            Result.success(workDataOf("uri" to uri))
        } catch (e: Exception) {
            if (runAttemptCount < 2) Result.retry()
            else Result.failure(workDataOf("error" to "Download failed: ${e.message ?: "network error"}"))
        }
    }

    private fun save(url: String, id: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 20_000
        conn.readTimeout = 30_000
        try {
            if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode}")
            val name = "infinity_${id.replace(Regex("[^A-Za-z0-9_-]"), "_")}.jpg"
            val resolver = applicationContext.contentResolver
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Infinity Wallpapers")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IOException("Cannot create file")
                try {
                    resolver.openOutputStream(uri)!!.use { out -> conn.inputStream.use { it.copyTo(out) } }
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                } catch (e: Exception) {
                    resolver.delete(uri, null, null)
                    throw e
                }
                uri.toString()
            } else {
                val dir = File(applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Infinity Wallpapers")
                dir.mkdirs()
                val f = File(dir, name)
                conn.inputStream.use { i -> f.outputStream().use { i.copyTo(it) } }
                Uri.fromFile(f).toString()
            }
        } finally {
            conn.disconnect()
        }
    }
}
