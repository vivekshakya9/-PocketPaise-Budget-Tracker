package com.vsdigitallabs.pocketpaise.stdbt.ui.components

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun AdMobBanner(context: Context, modifier: Modifier = Modifier) {
    val prefs = context.getSharedPreferences("PocketPaisePrefs", Context.MODE_PRIVATE)
    val bannerEnabled = prefs.getBoolean("banner_enabled", false)
    val testMode = prefs.getBoolean("admob_test_mode", true)
    
    // Fallback to test ID if test mode is on, or if no ID is set
    var unitId = prefs.getString("banner_unit_id", "") ?: ""
    if (testMode || unitId.isEmpty()) {
        unitId = "ca-app-pub-3940256099942544/6300978111" // Google Test Banner ID
    }

    if (bannerEnabled) {
        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = unitId
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}
