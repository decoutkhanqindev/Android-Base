package com.decoutkhanqindev.android_base.presentation.screens.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.decoutkhanqindev.android_base.presentation.effects.LaunchedWithLifecycleEffect
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MainScreen() {
    val viewModel: MainViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedWithLifecycleEffect {
        viewModel.effect.collect {
            // TODO: when (it) { ... } → navigate qua backStack, toast qua context.showToast(resources.getString(...))
        }
    }

    MainContent(state = state, onIntent = viewModel::onIntent)
}
