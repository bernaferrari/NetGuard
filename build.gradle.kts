plugins {
    base
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.koin.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.version.catalog.update)
}

tasks.named<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}