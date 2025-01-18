import com.filantrop.pvnclient.gradle.extensions.ksp

plugins {
    id("pvnclient.android.library.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.filantrop.pvnclient.auth.ui"
}

dependencies {

    implementation(project(":auth:data"))
    implementation(project(":auth:domain"))
    implementation(project(":core:model"))
    implementation(libs.androidx.activity)
    implementation(libs.androidx.compose.runtime.android)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.koin.android)
    implementation(libs.koin.core)

    ksp(libs.koin.ksp.compiler)
}
