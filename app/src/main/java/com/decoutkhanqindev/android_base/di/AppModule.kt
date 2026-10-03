package com.decoutkhanqindev.android_base.di

import com.decoutkhanqindev.android_base.ads.AdsManager
import com.decoutkhanqindev.android_base.data.local.datastore.DataStoreManager
import com.decoutkhanqindev.android_base.data.local.locale.LanguageManager
import com.decoutkhanqindev.android_base.data.network.connectivity.NetworkManager
import com.decoutkhanqindev.android_base.presentation.screens.main.MainViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val managerModule = module {
    single { DataStoreManager(androidApplication()) }
    single { LanguageManager(androidApplication()) }
    single { NetworkManager(androidApplication()) }
}

val adsModule = module {
    single { AdsManager() }
}

val repositoryModule = module {
    // TODO: DataSource + Repository của project — single<XxxRepository> { XxxRepositoryImpl(get()) }
}

val useCaseModule = module {
    // TODO: UseCase của project — factory { GetXxxUseCase(get()) }
}

val viewModelModule = module {
    viewModel { MainViewModel() }
}

val appModules = listOf(
    managerModule,
    adsModule,
    repositoryModule,
    useCaseModule,
    viewModelModule,
)
