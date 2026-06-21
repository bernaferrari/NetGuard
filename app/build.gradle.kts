import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose)
    alias(libs.plugins.koin.compiler)
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
    compileSdk = 37

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
        generateLocaleConfig = false
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
    implementation(project(":shared"))
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.appfunctions)
    implementation(libs.androidx.appfunctions.service)
    ksp(libs.androidx.appfunctions.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)
}

ksp {
    arg("appfunctions:aggregateAppFunctions", "true")
}
