import org.fptn.vpn.gradle.extensions.ksp

plugins {
    id("pvnclient.android.library.android")
    alias(libs.plugins.ksp)
}

android {
    namespace = "org.fptn.vpn.core.network"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.tracing.ktx)
    implementation(libs.koin.core)
    implementation(libs.kotlinx.coroutines.core)

    ksp(libs.koin.ksp.compiler)
}
