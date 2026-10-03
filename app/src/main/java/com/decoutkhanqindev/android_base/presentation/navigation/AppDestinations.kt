package com.decoutkhanqindev.android_base.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object SplashDestination : NavKey

@Serializable
data object MainDestination : NavKey

// TODO: Thêm destination của project — data object khi không có args, data class (field primitive/@Serializable) khi có args
