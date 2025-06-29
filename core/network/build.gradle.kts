import org.fptn.vpn.gradle.extensions.ksp

plugins {
    id("org.fptn.vpn.library.android")
    
}

android {
    namespace = "org.fptn.vpn.core.network"
}

dependencies {
    implementation(libs.koin.core)
    implementation(libs.kotlinx.coroutines.core)

    
}
