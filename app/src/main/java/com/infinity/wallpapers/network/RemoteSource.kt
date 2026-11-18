package com.infinity.wallpapers.network

import com.infinity.wallpapers.domain.FeedPage
import com.infinity.wallpapers.domain.WallCategory
import com.infinity.wallpapers.domain.WallpaperQuery

const val PAGE_SIZE = 24

interface RemoteSource {
    val name: String
    suspend fun load(q: WallpaperQuery, cursor: Any?): FeedPage
    suspend fun categories(): List<WallCategory> = emptyList()
}
