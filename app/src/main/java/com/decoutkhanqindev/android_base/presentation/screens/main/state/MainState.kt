package com.decoutkhanqindev.android_base.presentation.screens.main.state

import androidx.compose.runtime.Immutable

// TODO: State UI của màn Main — mọi field có default, list dùng ImmutableList
@Immutable
data class MainState(
    val isLoading: Boolean = false,
)
