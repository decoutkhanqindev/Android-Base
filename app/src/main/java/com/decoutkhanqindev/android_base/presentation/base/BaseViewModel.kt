package com.decoutkhanqindev.android_base.presentation.base

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

abstract class BaseViewModel<S, I, E>(initialState: S) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<E>(extraBufferCapacity = 1)
    val effect: SharedFlow<E> = _effect.asSharedFlow()

    abstract fun onIntent(intent: I)

    protected fun updateState(block: S.() -> S) = _state.update { it.block() }

    protected suspend fun sendEffect(effect: E) = _effect.emit(effect)
}
