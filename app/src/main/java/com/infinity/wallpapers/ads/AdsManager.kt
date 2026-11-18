package com.infinity.wallpapers.ads

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.infinity.wallpapers.BuildConfig

/**
 * Consent (Google UMP) -> MobileAds.initialize -> ads. No ad is requested before consent allows it.
 * Ads are never shown on the wallpaper detail / set flow.
 */
object AdsManager {
    var canRequestAds by mutableStateOf(false)
        private set
    private var initialized = false
    private var interstitial: InterstitialAd? = null
    private var detailCloses = 0
    private var lastShown = 0L

    fun start(activity: Activity) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        val appCtx = activity.applicationContext
        info.requestConsentInfoUpdate(
            activity, ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { initIfAllowed(appCtx, info) }
            },
            { initIfAllowed(appCtx, info) },
        )
        initIfAllowed(appCtx, info) // consent may already be stored from a previous session
    }

    private fun initIfAllowed(ctx: Context, info: ConsentInformation) {
        if (!info.canRequestAds() || initialized) return
        initialized = true
        MobileAds.initialize(ctx) { loadInterstitial(ctx) }
        canRequestAds = true
    }

    fun privacyOptionsRequired(ctx: Context): Boolean =
        UserMessagingPlatform.getConsentInformation(ctx).privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { }
    }

    private fun loadInterstitial(ctx: Context) {
        InterstitialAd.load(ctx, BuildConfig.ADMOB_INTERSTITIAL, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad }
                override fun onAdFailedToLoad(e: LoadAdError) { interstitial = null }
            })
    }

    /** Call when the user leaves a wallpaper detail screen (a natural navigation break). */
    fun onDetailClosed(activity: Activity) {
        detailCloses++
        val now = System.currentTimeMillis()
        if (!canRequestAds || detailCloses % 4 != 0 || now - lastShown < 120_000) return
        val ad = interstitial ?: return
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { interstitial = null; loadInterstitial(activity.applicationContext) }
            override fun onAdFailedToShowFullScreenContent(e: AdError) { interstitial = null; loadInterstitial(activity.applicationContext) }
        }
        lastShown = now
        ad.show(activity)
    }

    /** Optional rewarded ad ("support the developer"). Never required for any feature. */
    fun showRewarded(activity: Activity, onResult: (String) -> Unit) {
        if (!canRequestAds) { onResult("Ads are unavailable right now."); return }
        RewardedAd.load(activity, BuildConfig.ADMOB_REWARDED, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    ad.show(activity) { onResult("Thank you for supporting Infinity!") }
                }
                override fun onAdFailedToLoad(e: LoadAdError) { onResult("No ad available right now. Please try later.") }
            })
    }
}
