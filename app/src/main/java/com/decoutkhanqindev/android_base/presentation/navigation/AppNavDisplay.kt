package com.decoutkhanqindev.android_base.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.decoutkhanqindev.android_base.data.network.connectivity.NetworkManager
import com.decoutkhanqindev.android_base.presentation.components.dialog.NoInternetDialog
import com.decoutkhanqindev.android_base.presentation.screens.main.MainScreen
import com.decoutkhanqindev.android_base.presentation.screens.splash.SplashScreen
import org.koin.compose.koinInject
import timber.log.Timber

@Composable
fun AppNavDisplay(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(SplashDestination)
    val networkManager: NetworkManager = koinInject()
    val isNetworkAvailable by networkManager.isAvailable.collectAsStateWithLifecycle()

    NavDisplay(
        entries = rememberDecoratedNavEntries(
            backStack = backStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<SplashDestination> { SplashScreen(backStack) }
                entry<MainDestination> { MainScreen() }
                // TODO: Đăng ký entry<XxxDestination> { dest -> XxxScreen(...) } cho màn mới
            },
        ),
        modifier = modifier,
        onBack = {
            if (backStack.size > 1) {
                Timber.tag("Navigation").d("onBack, removing ${backStack.lastOrNull()?.let { it::class.simpleName }}")
                backStack.removeLastOrNull()
            }
        },
    )

    if (!isNetworkAvailable) NoInternetDialog()
}
