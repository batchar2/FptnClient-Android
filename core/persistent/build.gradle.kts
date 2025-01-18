import com.filantrop.pvnclient.gradle.extensions.ksp

plugins {
    id("pvnclient.android.library.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.filantrop.pvnclient.core.persistent"
}

dependencies {

    implementation(libs.datastore)
    implementation(libs.koin.annotations.jvm)
    implementation(libs.koin.core)

    ksp(libs.koin.ksp.compiler)
}
