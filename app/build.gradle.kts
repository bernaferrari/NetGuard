import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
val hasKeystore = keystorePropertiesFile.exists().also { exists ->
    if (exists) {
        keystoreProperties.load(keystorePropertiesFile.inputStream())
    }
}

android {
    namespace = "com.bernaferari.renetguard"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bernaferari.renetguard"
        versionName = "1"
        minSdk = 26
        targetSdk = 36
        versionCode = 1

        externalNativeBuild {
            cmake {
                cppFlags += ""
                arguments += listOf("-DANDROID_PLATFORM=android-26")
            }
        }

        ndkVersion = "25.2.9519653"
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }

        packaging {
            jniLibs {
                useLegacyPackaging = true
            }
        }
    }

    signingConfigs {
        if (hasKeystore) {
            create("release") {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("CMakeLists.txt")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    androidResources {
        generateLocaleConfig = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(file("proguard-rules.pro"))
            if (hasKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
            buildConfigField("boolean", "PLAY_STORE_RELEASE", "false")
            buildConfigField(
                "String",
                "HOSTS_FILE_URI",
                "\"https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts\"",
            )
            buildConfigField(
                "String",
                "GITHUB_LATEST_API",
                "\"https://api.github.com/repos/bernaferrari/NetGuard/releases/latest\"",
            )
        }
        create("play") {
            isMinifyEnabled = true
            if (hasKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(file("proguard-rules.pro"))
            buildConfigField("boolean", "PLAY_STORE_RELEASE", "true")
            buildConfigField("String", "HOSTS_FILE_URI", "\"\"")
            buildConfigField("String", "GITHUB_LATEST_API", "\"\"")
        }
        getByName("debug") {
            isMinifyEnabled = false

            proguardFiles(file("proguard-rules.pro"))
            buildConfigField("boolean", "PLAY_STORE_RELEASE", "false")
            buildConfigField(
                "String",
                "HOSTS_FILE_URI",
                "\"https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts\"",
            )
            buildConfigField(
                "String",
                "GITHUB_LATEST_API",
                "\"https://api.github.com/repos/bernaferrari/NetGuard/releases/latest\"",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        aidl = true
        compose = true
        buildConfig = true
    }

    lint {
        disable.add("MissingTranslation")
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.compose.material3.adaptive.navigation3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.google.material)
    implementation(libs.materialkolor)
    implementation(libs.hilt.android)
    implementation(libs.androidx.foundation)
    ksp(libs.hilt.android.compiler)
    implementation(libs.glide) {
        exclude(group = "com.android.support")
    }
    annotationProcessor(libs.glide.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
