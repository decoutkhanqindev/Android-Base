package com.decoutkhanqindev.android_base.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.decoutkhanqindev.android_base.BuildConfig
import com.decoutkhanqindev.android_base.ads.ad_unit.InterstitialAdUnit
import com.decoutkhanqindev.android_base.data.network.connectivity.NetworkManager
import com.decoutkhanqindev.android_base.utils.Tag
import com.decoutkhanqindev.android_base.utils.collectCatching
import com.decoutkhanqindev.android_base.utils.withContextCatching
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

class AdsManager : KoinComponent, Application.ActivityLifecycleCallbacks, Tag {

    private val application: Application by inject()
    private val networkManager: NetworkManager by inject()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val testDeviceIds = BuildConfig.ADMOB_TEST_DEVICE_IDS.split(',').filter { it.isNotBlank() }
    private val consentInformation: ConsentInformation by lazy {
        UserMessagingPlatform.getConsentInformation(application)
    }
    private val isConsentRequested = AtomicBoolean(false)
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)

    private val _isConsentGathered = MutableStateFlow(false)
    val isConsentGathered: StateFlow<Boolean> = _isConsentGathered.asStateFlow()

    private val _isMobileAdsInitialized = MutableStateFlow(false)
    val isMobileAdsInitialized: StateFlow<Boolean> = _isMobileAdsInitialized.asStateFlow()

    val canRequestAds: Boolean get() = consentInformation.canRequestAds()
    val isNetworkAvailable: Boolean get() = networkManager.isAvailable.value

    var isAdShowing: Boolean = false
        internal set

    private var currentActivity: Activity? = null

    val interSplash: InterstitialAdUnit by lazy {
        InterstitialAdUnit(floors = listOf(BuildConfig.INTER_SPLASH_ALL_ID to "inter_splash_all"))
    }

    // TODO: Thêm placement của project theo mẫu interSplash — mỗi placement 1 `by lazy`, id riêng `BuildConfig.<PLACEMENT>_ALL_ID`
    //  (khai báo ở cả release lẫn debug trong app/build.gradle.kts), floors xếp high floor trước: listOf(HIGH_ID to "x_high", ALL_ID to "x_all")

    init {
        application.registerActivityLifecycleCallbacks(this)
    }

    fun requestConsent(activity: Activity) {
        if (!isConsentRequested.compareAndSet(false, true)) return
        scope.launch {
            networkManager.isAvailable.collectCatching(
                action = {
                    if (!it || isMobileAdsInitializeCalled.get()) return@collectCatching
                    gatherConsent(activity)
                },
                catch = { Timber.tag(tag).w("Network availability check failed: ${it.message}") },
            )
        }
    }

    private fun gatherConsent(activity: Activity) {
        if (canRequestAds) initializeMobileAds()

        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .apply {
                if (BuildConfig.DEBUG) {
                    setConsentDebugSettings(
                        ConsentDebugSettings.Builder(activity)
                            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                            .apply { testDeviceIds.forEach(::addTestDeviceHashedId) }
                            .build()
                    )
                }
            }
            .build()

        Timber.tag(tag).d("Requesting consent info update with params: $params")
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                    currentActivity ?: activity
                ) { formError ->
                    formError?.let { Timber.tag(tag).w("Consent form: ${it.errorCode} ${it.message}") }
                    consentGatheringComplete()
                }
            },
            { requestError ->
                Timber.tag(tag).w("Consent info update: ${requestError.errorCode} ${requestError.message}")
                consentGatheringComplete()
            },
        )
    }

    private fun consentGatheringComplete() {
        if (canRequestAds) initializeMobileAds()
        _isConsentGathered.value = true
    }

    private fun initializeMobileAds() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) return

        if (testDeviceIds.isNotEmpty()) {
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    .setTestDeviceIds(testDeviceIds)
                    .build()
            )
        }

        scope.launch {
            withContextCatching(
                context = Dispatchers.IO,
                action = {
                    Timber.tag(tag).d("Initializing MobileAds...")
                    MobileAds.initialize(application) { status ->
                        Timber.tag(tag).d("MobileAds initialized: ${status.adapterStatusMap}")
                        _isMobileAdsInitialized.value = true
                    }
                },
                catch = {
                    Timber.tag(tag).w("MobileAds initialization failed: ${it.message}")
                    _isMobileAdsInitialized.value = false
                },
            )
        }
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {
        if (currentActivity == activity) currentActivity = null
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) currentActivity = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
