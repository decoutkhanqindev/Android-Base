package com.decoutkhanqindev.android_base

import android.app.Application
import com.decoutkhanqindev.android_base.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        setupTimber()
        setupKoin()
        // TODO: Khởi tạo SDK khác theo project (Firebase, ...) — việc nặng chạy nền, không block main thread
    }

    private fun setupTimber() {
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
    }

    private fun setupKoin() {
        startKoin {
            androidContext(this@App)
            modules(appModules)
        }
    }
}
