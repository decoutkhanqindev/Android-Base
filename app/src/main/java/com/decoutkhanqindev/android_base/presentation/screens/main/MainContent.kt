package com.decoutkhanqindev.android_base.presentation.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.decoutkhanqindev.android_base.presentation.screens.main.state.MainIntent
import com.decoutkhanqindev.android_base.presentation.screens.main.state.MainState

@Composable
fun MainContent(
    state: MainState,
    onIntent: (MainIntent) -> Unit,
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        // TODO: UI màn Main — render từ `state`, user action gọi `onIntent(...)`; cần bottom tab → CLAUDE.md › Navigation 3 › Nested navigation
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}
