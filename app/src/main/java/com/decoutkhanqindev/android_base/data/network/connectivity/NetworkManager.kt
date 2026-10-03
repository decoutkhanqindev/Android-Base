package com.decoutkhanqindev.android_base.data.network.connectivity

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.decoutkhanqindev.android_base.utils.Tag
import com.decoutkhanqindev.android_base.utils.recoverCatching
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber

class NetworkManager(
    private val app: Application,
) : Tag {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @OptIn(FlowPreview::class)
    val isAvailable: StateFlow<Boolean> = callbackFlow {
        val connectivityManager = app.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                trySend(networkCapabilities.hasInternetAccess())
            }

            // activeNetwork lúc này vẫn trả về network đang mất (còn VALIDATED) → phải gửi false thẳng.
            override fun onLost(network: Network) {
                trySend(false)
            }
        }

        trySend(connectivityManager.isInternetAvailable())
        connectivityManager.registerDefaultNetworkCallback(callback)
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }
        .debounce { isAvailable -> if (isAvailable) 0L else NETWORK_LOST_DEBOUNCE_MILLIS }
        .distinctUntilChanged()
        .recoverCatching { throwable ->
            Timber.tag(tag).e("isAvailable have error: ${throwable.stackTraceToString()}")
            emit(true)
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = true,
        )

    private fun ConnectivityManager.isInternetAvailable(): Boolean =
        activeNetwork?.let(::getNetworkCapabilities)?.hasInternetAccess() ?: false

    private fun NetworkCapabilities.hasInternetAccess(): Boolean =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

    companion object {
        private const val NETWORK_LOST_DEBOUNCE_MILLIS = 500L
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
