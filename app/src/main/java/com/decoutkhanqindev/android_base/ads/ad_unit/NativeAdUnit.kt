package com.decoutkhanqindev.android_base.ads.ad_unit

import android.content.Context
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class NativeAdUnit(
    floors: List<Pair<String, String>>,
) : AdUnit(floors) {

    private var _nativeAd: NativeAd? = null
    val nativeAd: NativeAd? get() = _nativeAd

    override fun requestLoad(context: Context, generation: Int) {
        scope.launch {
            if (!isCurrentGeneration(generation)) return@launch

            _state.value = AdUnitState.LOADING
            log("Loading")

            try {
                val ad = withTimeout(LOAD_TIMEOUT) { awaitLoad(context) }
                if (!isCurrentGeneration(generation)) {
                    ad.destroy()
                    return@launch
                }
                log("Loaded")
                _nativeAd?.destroy()
                _nativeAd = ad
                _state.value = AdUnitState.LOADED
            } catch (e: TimeoutCancellationException) {
                if (!isCurrentGeneration(generation)) return@launch
                log("Timeout")
                onLoadFailed(context, generation)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!isCurrentGeneration(generation)) return@launch
                log("Failed: ${e.message}")
                onLoadFailed(context, generation)
            }
        }
    }

    private suspend fun awaitLoad(context: Context): NativeAd =
        suspendCancellableCoroutine { cont ->
            AdLoader.Builder(context, currentId)
                .forNativeAd { ad ->
                    if (cont.isActive) cont.resume(ad)
                }
                .withAdListener(
                    object : AdListener() {
                        override fun onAdFailedToLoad(error: LoadAdError) {
                            if (cont.isActive) cont.resumeWithException(Exception(error.message))
                        }

                        override fun onAdImpression() {
                            log("Impression")
                            _state.value = AdUnitState.IMPRESSION
                        }
                    }
                )
                .build()
                .loadAd(AdRequest.Builder().build())
        }

    override fun releaseAd() {
        _nativeAd?.destroy()
        _nativeAd = null
    }
}
