package com.infinity.wallpapers.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow

class AppSettings(ctx: Context) {
    private val sp = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)

    val theme = MutableStateFlow(sp.getString("theme", "system") ?: "system")      // system | light | dark
    val quality = MutableStateFlow(sp.getString("quality", "standard") ?: "standard") // standard | high
    val wifiOnly = MutableStateFlow(sp.getBoolean("wifiOnly", false))

    var onboarded: Boolean
        get() = sp.getBoolean("onboarded", false)
        set(v) = sp.edit { putBoolean("onboarded", v) }

    fun setTheme(v: String) { sp.edit { putString("theme", v) }; theme.value = v }
    fun setQuality(v: String) { sp.edit { putString("quality", v) }; quality.value = v }
    fun setWifiOnly(v: Boolean) { sp.edit { putBoolean("wifiOnly", v) }; wifiOnly.value = v }
}
