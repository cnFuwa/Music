import java.util.Properties
import java.io.FileInputStream

val propsFile = rootProject.file("keystore.properties")
val localProps = if (propsFile.exists()) {
    Properties().apply { load(FileInputStream(propsFile)) }
} else null

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "cn.primeessence.music"
    compileSdk { version = release(36) }

    defaultConfig {
        applicationId = "cn.primeessence.music"
        minSdk { version = release(29) }
        targetSdk { version = release(36) }
        versionCode = 1
        versionName = "1.0.0"
    }
    
    signingConfigs {
        create("release") {
            storeFile = file("${rootProject.projectDir}/keystore/music_release.p12")
            storePassword = System.getenv("KEYSTORE_PASS") ?: localProps?.getProperty("keystore.storePass")
            keyAlias = System.getenv("KEY_ALIAS") ?: localProps?.getProperty("keystore.alias")
            keyPassword = System.getenv("KEY_PASS") ?: localProps?.getProperty("keystore.keyPass")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.activity.compose)

    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.coroutines.android)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.common)

    implementation(libs.documentfile)

}
