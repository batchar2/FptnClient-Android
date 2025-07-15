plugins {
    id("org.fptn.vpn.library.android")
}

android {
    namespace = "org.fptn.vpn.auth.data"
}

dependencies {

    implementation(project(":auth:domain"))
    implementation(project(":core:persistent"))

    implementation(libs.koin.core)
    implementation(libs.kotlinx.coroutines.core)
}
