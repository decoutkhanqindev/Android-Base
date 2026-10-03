package com.decoutkhanqindev.android_base.presentation.screens.main

import com.decoutkhanqindev.android_base.presentation.base.BaseViewModel
import com.decoutkhanqindev.android_base.presentation.screens.main.state.MainEffect
import com.decoutkhanqindev.android_base.presentation.screens.main.state.MainIntent
import com.decoutkhanqindev.android_base.presentation.screens.main.state.MainState
import com.decoutkhanqindev.android_base.utils.Tag
import timber.log.Timber

class MainViewModel : BaseViewModel<MainState, MainIntent, MainEffect>(
    initialState = MainState(),
), Tag {

    override fun onIntent(intent: MainIntent) {
        Timber.tag(tag).d("onIntent: $intent")
        // TODO: when (intent) { ... } → updateState { copy(...) } / viewModelScope.launch { sendEffect(...) }; UseCase inject qua constructor
    }
}
