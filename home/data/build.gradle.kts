import org.fptn.vpn.gradle.extensions.ksp

plugins {
    id("pvnclient.android.library.android")
    alias(libs.plugins.ksp)
}

android {
    namespace = "org.fptn.vpn.home.data"
}

dependencies {
    ksp(libs.koin.ksp.compiler)
}
