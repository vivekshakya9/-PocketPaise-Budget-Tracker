package com.vsdigitallabs.pocketpaise.stdbt.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun AdMobBanner(
    bannerEnabled: Boolean,
    bannerUnitId: String,
    modifier: Modifier = Modifier
) {
    if (!bannerEnabled) return

    val finalUnitId = if (bannerUnitId.isBlank()) {
        "ca-app-pub-3940256099942544/6300978111" // Google test banner ID fallback
    } else {
        bannerUnitId
    }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = finalUnitId
                loadAd(AdRequest.Builder().build())
            }
        },
        update = { adView ->
            // If unit ID changed, reload the ad
            if (adView.adUnitId != finalUnitId) {
                adView.adUnitId = finalUnitId
                adView.loadAd(AdRequest.Builder().build())
            }
        }
    )
}
