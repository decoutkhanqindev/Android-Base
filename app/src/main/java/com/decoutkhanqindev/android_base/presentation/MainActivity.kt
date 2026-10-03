package com.decoutkhanqindev.android_base.presentation

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.decoutkhanqindev.android_base.ads.AdsManager
import com.decoutkhanqindev.android_base.data.local.datastore.DataStoreManager
import com.decoutkhanqindev.android_base.data.local.locale.LanguageManager
import com.decoutkhanqindev.android_base.presentation.model.LanguageValue
import com.decoutkhanqindev.android_base.presentation.navigation.AppNavDisplay
import com.decoutkhanqindev.android_base.presentation.theme.AppTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val dataStoreManager: DataStoreManager by inject()
    private val languageManager: LanguageManager by inject()
    private val adsManager: AdsManager by inject()

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        ComposeUiFlags.isBypassUnfocusableComposeViewEnabled = false
        super.onCreate(savedInstanceState)
        adsManager.requestConsent(this)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        setContent {
            val selectedLangCode by dataStoreManager.selectedLangCode.collectAsStateWithLifecycle()
            val languageCode = LanguageValue.fromCode(selectedLangCode).code
            val configuration = remember(languageCode) {
                languageManager.configurationFor(languageCode)
            }
            val resources = remember(configuration) {
                languageManager.resourcesFor(configuration)
            }

            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalResources provides resources,
            ) {
                AppTheme {
                    AppNavDisplay(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
