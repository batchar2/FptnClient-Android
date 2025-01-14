import com.filantrop.pvnclient.gradle.extensions.ksp

plugins {
    id("pvnclient.android.library.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.filantrop.pvnclient.auth.ui"
}

dependencies {

    implementation(libs.androidx.compose.runtime.android)
    implementation(libs.koin.core)

    ksp(libs.koin.ksp.compiler)
}
