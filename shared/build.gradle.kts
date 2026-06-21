@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.koin.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidLibrary {
        namespace = "com.bernaferari.renetguard.shared"
        compileSdk = 37
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
        androidResources {
            enable = true
        }
        withHostTest {}
    }

    wasmJs {
        browser()
    }

    compilerOptions {
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi",
            "androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi",
        )
    }

    sourceSets {
        val composeGeneratedKotlin = layout.buildDirectory.dir("generated/compose/resourceGenerator/kotlin")
        fun org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.addComposeGeneratedSources(vararg dirs: String) {
            dirs.forEach { dir -> kotlin.srcDir(composeGeneratedKotlin.map { it.dir(dir) }) }
        }

        commonMain {
            addComposeGeneratedSources(
                "commonResClass",
                "commonMainResourceAccessors",
                "commonMainResourceCollectors",
            )
        }
        androidMain { addComposeGeneratedSources("androidMainResourceCollectors") }
        wasmJsMain { addComposeGeneratedSources("wasmJsMainResourceCollectors") }

        commonMain.dependencies {
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.androidx.datastore.core)
            implementation(libs.androidx.datastore.preferences.core)
            implementation(libs.kotlinx.atomicfu)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.components.resources)
            implementation(libs.material.kolor)
            implementation(libs.jetbrains.navigation3.ui)
            implementation(libs.compose.adaptive.navigation3)
            implementation(libs.compose.adaptive.navigation.suite)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.lifecycle.viewmodel.navigation3)
            implementation(libs.koin.core)
            implementation(libs.koin.annotations)
            implementation(libs.koin.core.viewmodel)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
        }

        androidMain.dependencies {
            api(libs.androidx.core.ktx)
            api(libs.androidx.activity.compose)
            api(libs.androidx.compose.runtime)
            api(libs.androidx.compose.foundation)
            api(libs.androidx.compose.ui)
            api(libs.androidx.compose.material.icons.extended)
            api(libs.androidx.compose.material3)
            api(libs.androidx.navigation3.runtime)
            api(libs.androidx.navigation3.ui)
            api(libs.androidx.lifecycle.viewmodel.navigation3)
            api(libs.androidx.compose.material3.adaptive.navigation3)
            api(libs.androidx.compose.material3.adaptive.navigation.suite)
            api(libs.androidx.lifecycle.runtime.compose)
            api(libs.androidx.lifecycle.viewmodel.compose)
            api(libs.kotlinx.coroutines.android)
            api(libs.materialkolor)
            api(libs.koin.annotations)
            api(libs.koin.core)
            api(libs.koin.android)
            api(libs.koin.androidx.compose)
            api(libs.coil.compose)
            implementation(libs.androidx.datastore.preferences)
        }

        wasmJsMain.dependencies {
            implementation(libs.okio)
            implementation("com.squareup.okio:okio-fakefilesystem:${libs.versions.okio.get()}")
            implementation(libs.androidx.datastore.preferences.core)
        }

        val androidHostTest by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.androidx.datastore.preferences)
                implementation(libs.junit)
            }
        }
    }
}

dependencies {
    add("androidMainApi", platform(libs.androidx.compose.bom))
    add("androidMainApi", platform(libs.koin.bom))
}

compose.resources {
    generateResClass = org.jetbrains.compose.resources.ResourcesExtension.ResourceClassGeneration.Always
}