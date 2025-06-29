import org.fptn.vpn.gradle.extensions.ksp

plugins {
    id("pvnclient.android.library.android")
    id("pvnclient.android.library.android.compose")
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "org.fptn.vpn.home.ui"
}

dependencies {

    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.runtime.android)
    implementation(libs.androidx.navigation.runtime)
    implementation(libs.kotlinx.serialization.core.jvm)

    ksp(libs.koin.ksp.compiler)
}
