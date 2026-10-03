import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.decoutkhanqindev.android_base"
    compileSdk = 37
    defaultConfig {
        // TODO: Đổi applicationId + versionCode/versionName theo project (đổi namespace/package: README › Tạo project mới)
        applicationId = "com.decoutkhanqindev.android_base"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "v1.0.0"

        buildConfigField(
            "String",
            "ADMOB_TEST_DEVICE_IDS",
            "\"${localProperties.getProperty("admob.test.device.ids", "")}\""
        )
        buildConfigField("String", "ADMOB_BANNER_TEST_ID", "\"ca-app-pub-3940256099942544/9214589741\"")
        buildConfigField("String", "ADMOB_NATIVE_TEST_ID", "\"ca-app-pub-3940256099942544/2247696110\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_TEST_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
        buildConfigField("String", "ADMOB_REWARDED_TEST_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
        buildConfigField("String", "ADMOB_APP_OPEN_TEST_ID", "\"ca-app-pub-3940256099942544/9257395921\"")
    }

    signingConfigs {
        create("release") {
            localProperties.getProperty("signing.store.file")?.let { storeFile = file(it) }
            storePassword = localProperties.getProperty("signing.store.password")
            keyAlias = localProperties.getProperty("signing.key.alias")
            keyPassword = localProperties.getProperty("signing.key.password")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // TODO: Thay bằng ad unit id thật của từng placement (debug giữ test id của Google)
            buildConfigField("String", "INTER_SPLASH_ALL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
        }

        debug {
            buildConfigField("String", "INTER_SPLASH_ALL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        aidl = false
        buildConfig = true
        shaders = false
    }

    androidResources {
        generateLocaleConfig = true
    }

    // Đổi ngôn ngữ trong app → AAB phải chứa đủ mọi ngôn ngữ (Play không tải thêm split ngôn ngữ khác ngôn ngữ máy).
    bundle {
        language {
            enableSplit = false
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Arch Components
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // Tooling
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Instrumented tests
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Local tests: jUnit, coroutines, Android runner
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // Instrumented tests: jUnit rules and runners
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)

    // Navigation
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Icons Extended
    implementation(libs.androidx.compose.material.icons.extended)

    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // Timber
    implementation(libs.timber)

    // Immutable Collections
    implementation(libs.kotlinx.collections.immutable)

    // Lottie
    implementation(libs.lottie.compose)

    // Preferences DataStore
    implementation(libs.androidx.datastore.preferences)

    // Ads (AdMob + UMP consent; ConstraintLayout cho layout XML của NativeAdView)
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)
    implementation(libs.androidx.constraintlayout)
}
