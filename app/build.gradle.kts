plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.meteoapp"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.meteoapp"
        minSdk = 26
        targetSdk = 37
        versionCode = 13
        versionName = "1.10.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val debugStorePassword = System.getenv("DEBUG_KEYSTORE_PASSWORD") ?: "android"
    val debugKeyAlias = System.getenv("DEBUG_KEY_ALIAS") ?: "androiddebugkey"
    val debugKeyPassword = System.getenv("DEBUG_KEY_PASSWORD") ?: "android"
    val debugKeystorePath = System.getenv("DEBUG_KEYSTORE_PATH")

    signingConfigs {
        create("sharedDebug") {
            if (debugKeystorePath != null) {
                storeFile = file(debugKeystorePath)
                storePassword = debugStorePassword
                keyAlias = debugKeyAlias
                keyPassword = debugKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            if (debugKeystorePath != null) {
                signingConfig = signingConfigs.getByName("sharedDebug")
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (debugKeystorePath != null) {
                signingConfig = signingConfigs.getByName("sharedDebug")
            } else {
                signingConfig = signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }


    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)

    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.security.crypto)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi)
    ksp(libs.moshi.kotlin.codegen)


    implementation(libs.play.services.location)

    implementation(libs.osmdroid.android)
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
