package com.vsdigitallabs.pocketpaise.stdbt.utils

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date

class AppOpenAdManager(private val myApplication: Application) : Application.ActivityLifecycleCallbacks, LifecycleEventObserver {

    private var appOpenAd: AppOpenAd? = null
    private var isShowingAd = false
    private var loadTime: Long = 0
    private var currentActivity: Activity? = null

    init {
        myApplication.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    private fun loadAd() {
        val prefs = myApplication.getSharedPreferences("PocketPaisePrefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("app_open_enabled", false)
        val unitId = prefs.getString("app_open_unit_id", "") ?: ""

        if (!isEnabled || unitId.isEmpty() || isAdAvailable()) {
            return
        }

        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            myApplication, unitId, request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    loadTime = Date().time
                    Log.d("AdMob", "App Open Ad loaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.d("AdMob", "App Open Ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
    }

    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    private fun showAdIfAvailable() {
        if (!isShowingAd && isAdAvailable()) {
            val activity = currentActivity ?: return
            
            val prefs = myApplication.getSharedPreferences("PocketPaisePrefs", Context.MODE_PRIVATE)
            val isEnabled = prefs.getBoolean("app_open_enabled", false)
            if (!isEnabled) return

            appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    isShowingAd = false
                    loadAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    appOpenAd = null
                    isShowingAd = false
                    loadAd()
                }

                override fun onAdShowedFullScreenContent() {
                    isShowingAd = true
                }
            }
            isShowingAd = true
            appOpenAd?.show(activity)
        } else {
            loadAd()
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_START) {
            showAdIfAvailable()
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) { currentActivity = activity }
    override fun onActivityResumed(activity: Activity) { currentActivity = activity }
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }
}
