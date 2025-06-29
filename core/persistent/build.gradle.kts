import org.fptn.vpn.gradle.extensions.ksp

plugins {
    id("pvnclient.android.library.android")
    alias(libs.plugins.ksp)
}

android {
    namespace = "org.fptn.vpn.core.persistent"
}

dependencies {

    implementation(libs.datastore)
    implementation(libs.koin.core)

    ksp(libs.koin.ksp.compiler)
}
