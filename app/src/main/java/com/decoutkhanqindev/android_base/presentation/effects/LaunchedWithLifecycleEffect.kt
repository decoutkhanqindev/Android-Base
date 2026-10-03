package com.decoutkhanqindev.android_base.presentation.effects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

@Composable
fun LaunchedWithLifecycleEffect(
    state: Lifecycle.State = Lifecycle.State.STARTED,
    block: suspend () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestBlock by rememberUpdatedState(block)

    LaunchedEffect(lifecycleOwner, state) {
        lifecycleOwner.repeatOnLifecycle(state) {
            latestBlock()
        }
    }
}