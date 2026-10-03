package com.decoutkhanqindev.android_base.presentation.screens.splash

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.decoutkhanqindev.android_base.ads.AdsManager
import com.decoutkhanqindev.android_base.ads.ad_unit.AdUnitState
import com.decoutkhanqindev.android_base.data.network.connectivity.NetworkManager
import com.decoutkhanqindev.android_base.presentation.navigation.MainDestination
import com.decoutkhanqindev.android_base.utils.navigateTo
import org.koin.compose.koinInject

@Composable
fun SplashScreen(backStack: NavBackStack<NavKey>) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val networkManager: NetworkManager = koinInject()
    val adsManager: AdsManager = koinInject()
    val isNetworkAvailable by networkManager.isAvailable.collectAsStateWithLifecycle()
    val isConsentGathered by adsManager.isConsentGathered.collectAsStateWithLifecycle()
    val isMobileAdsInitialized by adsManager.isMobileAdsInitialized.collectAsStateWithLifecycle()
    val interSplash = adsManager.interSplash
    val interSplashState by interSplash.state.collectAsStateWithLifecycle()
    val handleNext = {
        // TODO: Lần đầu mở app (DataStoreManager.isFirstOpen) → màn Language/Onboarding của project
        backStack.navigateTo(MainDestination, preserveState = false)
    }

    LaunchedEffect(isConsentGathered, isMobileAdsInitialized, isNetworkAvailable) {
        when {
            !isConsentGathered || !isNetworkAvailable -> Unit
            !adsManager.canRequestAds -> handleNext()
            isMobileAdsInitialized -> interSplash.load(context)
        }
    }

    LifecycleResumeEffect(interSplashState, isNetworkAvailable) {
        if (isNetworkAvailable) {
            when (interSplashState) {
                AdUnitState.LOADED -> activity?.let {
                    interSplash.show(
                        activity = it,
                        onAdShowed = handleNext,
                        onAdFailedToShow = handleNext,
                    )
                } ?: handleNext()

                AdUnitState.FAILED -> handleNext()

                else -> Unit
            }
        }

        onPauseOrDispose { }
    }

    BackHandler { }

    SplashContent()
}
