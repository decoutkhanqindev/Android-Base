package com.decoutkhanqindev.android_base.ads.ad_unit

import android.content.Context
import com.decoutkhanqindev.android_base.ads.AdsManager
import com.decoutkhanqindev.android_base.utils.Tag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

abstract class AdUnit(
    private val floors: List<Pair<String, String>>,
) : KoinComponent, Tag {

    private val adsManager: AdsManager by inject()

    // Mobile Ads SDK bắt buộc load/show trên main thread → scope riêng của unit chạy Dispatchers.Main.
    protected val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    protected var isAdShowing: Boolean
        get() = adsManager.isAdShowing
        set(value) {
            adsManager.isAdShowing = value
        }

    private var floorIndex = 0
    private var loadGeneration = 0

    protected val currentId: String get() = floors[floorIndex].first
    private val currentName: String get() = floors[floorIndex].second
    private val currentLabel: String get() = "$currentName ($currentId)"

    protected val _state = MutableStateFlow(AdUnitState.NONE)
    val state: StateFlow<AdUnitState> = _state.asStateFlow()

    fun load(context: Context) {
        if (_state.value == AdUnitState.LOADING || _state.value == AdUnitState.LOADED) return
        // Reset trước 2 gate để log của gate ghi đúng floor 0; KHÔNG đặt trên guard (đang LOADING sẽ tua floorIndex của request dở).
        resetWaterfall()
        if (!adsManager.canRequestAds) {
            log("Consent not granted, not loading")
            _state.value = AdUnitState.FAILED
            return
        }
        if (!adsManager.isNetworkAvailable) {
            log("No network, not loading")
            _state.value = AdUnitState.FAILED
            return
        }
        requestLoad(context, nextGeneration())
    }

    private fun resetWaterfall() {
        floorIndex = 0
    }

    private fun tryFallback(): Boolean {
        if (floorIndex + 1 >= floors.size) return false
        floorIndex++
        return true
    }

    private fun nextGeneration(): Int {
        loadGeneration++
        return loadGeneration
    }

    protected fun isCurrentGeneration(generation: Int): Boolean = generation == loadGeneration

    protected fun onLoadFailed(context: Context, generation: Int) {
        if (!isCurrentGeneration(generation)) return
        val failedLabel = currentLabel
        if (tryFallback()) {
            log("Falling back to $currentLabel", label = failedLabel)
            requestLoad(context, generation)
        } else {
            log("No fallback left, giving up")
            _state.value = AdUnitState.FAILED
        }
    }

    // Mọi log của unit đi qua đây để luôn kèm ad unit id của floor (1 placement có thể nhiều id).
    protected fun log(message: String, label: String = currentLabel) {
        Timber.tag(tag).d("$label - $message")
    }

    fun release() {
        log("Released")
        nextGeneration()
        releaseAd()
        _state.value = AdUnitState.NONE
    }

    fun destroy() {
        scope.cancel()
        release()
    }

    protected abstract fun requestLoad(context: Context, generation: Int)
    protected abstract fun releaseAd()

    companion object {
        const val LOAD_TIMEOUT = 20_000L
    }
}
