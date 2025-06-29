import org.fptn.vpn.gradle.extensions.ksp

plugins {
    id("pvnclient.android.library.android")
    alias(libs.plugins.ksp)
}

android {
    namespace = "org.fptn.vpn.auth.data"
}

dependencies {

    implementation(project(":auth:domain"))
    implementation(project(":core:persistent"))

    implementation(libs.koin.core)
    implementation(libs.kotlinx.coroutines.core)

    ksp(libs.koin.ksp.compiler)
}
