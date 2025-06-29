import org.fptn.vpn.gradle.extensions.ksp

plugins {
    id("org.fptn.vpn.library.android")
    
}

android {
    namespace = "org.fptn.vpn.core.persistent"
}

dependencies {

    implementation(libs.datastore)
    implementation(libs.koin.core)

    
}
