import com.filantrop.pvnclient.gradle.extensions.ksp

plugins {
    id("pvnclient.android.library.kotlin")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.koin.annotations.jvm)
    implementation(libs.koin.core)
    implementation(libs.kotlinx.coroutines.core)

    ksp(libs.koin.ksp.compiler)
}
