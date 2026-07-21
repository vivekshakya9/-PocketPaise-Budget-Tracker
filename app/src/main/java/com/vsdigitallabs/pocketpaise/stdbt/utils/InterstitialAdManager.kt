package com.vsdigitallabs.pocketpaise.stdbt.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object InterstitialAdManager {
    private var interstitialAd: InterstitialAd? = null
    private var isAdLoading = false

    fun loadAd(context: Context) {
        val prefs = context.getSharedPreferences("PocketPaisePrefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("interstitial_enabled", false)
        val unitId = prefs.getString("interstitial_unit_id", "") ?: ""

        if (!isEnabled || unitId.isEmpty() || interstitialAd != null || isAdLoading) {
            return
        }

        isAdLoading = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(context, unitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d("AdMob", "Interstitial Ad failed to load: ${adError.message}")
                interstitialAd = null
                isAdLoading = false
            }

            override fun onAdLoaded(ad: InterstitialAd) {
                Log.d("AdMob", "Interstitial Ad loaded")
                interstitialAd = ad
                isAdLoading = false
            }
        })
    }

    fun showAd(activity: Activity, onAdDismissed: () -> Unit) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    onAdDismissed()
                    loadAd(activity) // Pre-load next ad
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    onAdDismissed()
                }
            }
            interstitialAd?.show(activity)
        } else {
            onAdDismissed()
            loadAd(activity) // Try loading if not ready
        }
    }
}
