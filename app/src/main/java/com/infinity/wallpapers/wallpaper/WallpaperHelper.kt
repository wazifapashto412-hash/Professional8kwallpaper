package com.infinity.wallpapers.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import com.infinity.wallpapers.ServiceLocator
import com.infinity.wallpapers.domain.Wallpaper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

enum class WallTarget(val flag: Int, val label: String) {
    HOME(WallpaperManager.FLAG_SYSTEM, "Home screen"),
    LOCK(WallpaperManager.FLAG_LOCK, "Lock screen"),
    BOTH(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK, "Home and lock screens"),
}

/** Image to display / apply, honouring the user's quality setting and any local copy. */
fun Wallpaper.displayUrl(): String =
    localUri ?: if (ServiceLocator.settings.quality.value == "high") fullUrl else previewUrl

object WallpaperHelper {

    private suspend fun loadBitmap(ctx: Context, url: String, w: Int, h: Int): Bitmap {
        val req = ImageRequest.Builder(ctx)
            .data(url).size(w, h).scale(Scale.FILL)
            .allowHardware(false) // WallpaperManager needs a software bitmap
            .memoryCachePolicy(CachePolicy.DISABLED)
            .build()
        return when (val res = ctx.imageLoader.execute(req)) {
            is SuccessResult -> res.drawable.toBitmap()
            is ErrorResult -> throw IOException(res.throwable.message ?: "Image could not be loaded", res.throwable)
        }
    }

    /** Uses the official WallpaperManager API (SET_WALLPAPER is an install-time permission; no storage permission needed). */
    suspend fun set(ctx: Context, w: Wallpaper, target: WallTarget): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val wm = WallpaperManager.getInstance(ctx)
            check(wm.isWallpaperSupported) { "Wallpapers are not supported on this device." }
            check(wm.isSetWallpaperAllowed) { "Changing wallpaper is not allowed on this device or profile." }
            val dm = ctx.resources.displayMetrics
            val bmp = loadBitmap(ctx, w.displayUrl(), dm.widthPixels, dm.heightPixels)
            wm.setBitmap(bmp, null, true, target.flag)
            Unit
        }
    }

    /** Builds a share chooser with a downscaled copy in the app cache (shared via FileProvider). */
    suspend fun buildShareIntent(ctx: Context, w: Wallpaper): Result<Intent> = withContext(Dispatchers.IO) {
        runCatching {
            val bmp = loadBitmap(ctx, w.localUri ?: w.previewUrl, 1080, 1920)
            val dir = File(ctx.cacheDir, "shared").apply { mkdirs() }
            val f = File(dir, "wallpaper.jpg")
            f.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, listOfNotNull(w.title, w.credit, "Shared from Infinity 8K Wallpapers").joinToString("\n"))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Intent.createChooser(send, "Share wallpaper").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
