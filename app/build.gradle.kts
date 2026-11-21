import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.parcelize)
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
    }
}

android {
    namespace = "cn.primeessence.music"
    compileSdk = 37

    defaultConfig {
        applicationId = "cn.primeessence.music"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            val envPass = providers.environmentVariable("KEYSTORE_PASS").orNull
            val envAlias = providers.environmentVariable("KEY_ALIAS").orNull
            val envKey = providers.environmentVariable("KEY_PASS").orNull
            val envPath = providers.environmentVariable("KEYSTORE_PATH").orNull

            val props = Properties().apply {
                val f = rootProject.file("keystore.properties")
                if (f.exists()) FileInputStream(f).use { load(it) }
            }
            val propPass = props.getProperty("storePassword")
            val propAlias = props.getProperty("keyAlias")
            val propKey = props.getProperty("keyPassword")
            val propPath = props.getProperty("storeFile")

            val storeP = envPass ?: propPass
            val alias = envAlias ?: propAlias
            val keyP = envKey ?: propKey
            val path = envPath ?: propPath ?: "keystore/key.p12"

            when {
                storeP == null || alias == null || keyP == null -> error(
                    "release 签名缺失：请配置环境变量(KEYSTORE_PASS/KEY_ALIAS/KEY_PASS) " +
                        "或项目根目录 keystore.properties(storePassword/keyAlias/keyPassword)"
                )
                else -> {
                    storeType = "PKCS12"
                    storeFile = rootProject.file(path)
                    storePassword = storeP
                    keyAlias = alias
                    keyPassword = keyP
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    
    implementation(libs.android.material)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.icons.extended)

    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)

    implementation(libs.androidx.documentfile)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel)
}
