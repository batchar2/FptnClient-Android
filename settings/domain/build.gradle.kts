import org.fptn.vpn.gradle.extensions.ksp

plugins {
    id("pvnclient.android.library.kotlin")
    alias(libs.plugins.ksp)
}

dependencies {
    ksp(libs.koin.ksp.compiler)
}
