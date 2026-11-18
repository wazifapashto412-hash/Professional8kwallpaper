package com.infinity.wallpapers

import android.content.Context
import com.google.firebase.FirebaseApp
import com.infinity.wallpapers.data.AppDatabase
import com.infinity.wallpapers.data.AppSettings
import com.infinity.wallpapers.firebase.FirestoreSource
import com.infinity.wallpapers.network.DemoSource
import com.infinity.wallpapers.network.PexelsSource
import com.infinity.wallpapers.network.RemoteSource
import com.infinity.wallpapers.repository.WallpaperRepository

object ServiceLocator {
    lateinit var app: Context
        private set
    lateinit var settings: AppSettings
        private set
    lateinit var db: AppDatabase
        private set
    lateinit var repo: WallpaperRepository
        private set

    fun init(ctx: Context) {
        app = ctx.applicationContext
        settings = AppSettings(app)
        db = AppDatabase.create(app)
        val remote: RemoteSource = when {
            FirebaseApp.getApps(app).isNotEmpty() -> FirestoreSource()
            BuildConfig.PEXELS_API_KEY.isNotBlank() -> PexelsSource(BuildConfig.PEXELS_API_KEY)
            else -> DemoSource()
        }
        repo = WallpaperRepository(remote, db)
    }
}
