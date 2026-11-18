package com.infinity.wallpapers.ads

import android.content.Context
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.infinity.wallpapers.BuildConfig

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    if (!AdsManager.canRequestAds) return
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val widthDp = maxWidth.value.toInt()
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { c ->
                AdView(c).apply {
                    setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(c, widthDp))
                    adUnitId = BuildConfig.ADMOB_BANNER
                    loadAd(AdRequest.Builder().build())
                }
            },
            onRelease = { it.destroy() },
        )
    }
}

@Composable
fun NativeAdCard(modifier: Modifier = Modifier) {
    if (!AdsManager.canRequestAds) return
    val ctx = LocalContext.current
    var ad by remember { mutableStateOf<NativeAd?>(null) }
    DisposableEffect(Unit) {
        var loaded: NativeAd? = null
        val loader = AdLoader.Builder(ctx, BuildConfig.ADMOB_NATIVE)
            .forNativeAd { loaded?.destroy(); loaded = it; ad = it }
            .withAdListener(object : AdListener() {})
            .build()
        loader.loadAd(AdRequest.Builder().build())
        onDispose { loaded?.destroy() }
    }
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val current = ad ?: return
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { c -> buildNativeView(c, textColor) },
        update = { populate(it, current) },
    )
}

private fun buildNativeView(c: Context, textColor: Int): NativeAdView {
    val v = NativeAdView(c)
    val col = LinearLayout(c).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(32, 24, 32, 24)
    }
    val badge = TextView(c).apply { text = "Ad"; textSize = 11f; setTextColor(textColor) }
    val head = TextView(c).apply { textSize = 16f; setTypeface(null, Typeface.BOLD); setTextColor(textColor) }
    val media = MediaView(c).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 480)
    }
    val body = TextView(c).apply { textSize = 13f; setTextColor(textColor) }
    val cta = Button(c)
    listOf(badge, head, media, body, cta).forEach { col.addView(it) }
    v.addView(col)
    v.headlineView = head; v.bodyView = body; v.callToActionView = cta; v.mediaView = media
    return v
}

private fun populate(v: NativeAdView, n: NativeAd) {
    (v.headlineView as TextView).text = n.headline
    (v.bodyView as TextView).text = n.body ?: ""
    (v.callToActionView as Button).text = n.callToAction ?: "Open"
    v.mediaView?.mediaContent = n.mediaContent
    v.setNativeAd(n)
}
