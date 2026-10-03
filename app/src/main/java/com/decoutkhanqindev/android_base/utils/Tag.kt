package com.decoutkhanqindev.android_base.utils

interface Tag {
    val tag: String get() = this::class.simpleName ?: ""
}
