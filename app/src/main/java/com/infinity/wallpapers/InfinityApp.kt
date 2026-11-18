package com.infinity.wallpapers

import android.app.Application

class InfinityApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
