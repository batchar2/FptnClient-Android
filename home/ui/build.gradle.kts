import com.filantrop.pvnclient.gradle.extensions.ksp

plugins {
    id("pvnclient.android.library.android")
    id("pvnclient.android.library.android.compose")
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.filantrop.pvnclient.home.ui"
}

dependencies {

    implementation(project(":home:data"))
    implementation(project(":home:domain"))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.runtime.android)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.koin.core)
    implementation(libs.koin.core.viewmodel.jvm)
    implementation(libs.kotlinx.serialization.json)

    ksp(libs.koin.ksp.compiler)
}
