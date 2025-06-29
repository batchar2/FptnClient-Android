import org.fptn.vpn.gradle.extensions.ksp

plugins {
    id("pvnclient.android.library.android")
    id("pvnclient.android.library.android.compose")
    alias(libs.plugins.ksp)
}

android {
    namespace = "org.fptn.vpn.core.designsystem"
}

dependencies {
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.core)

    debugImplementation(libs.androidx.compose.ui.tooling)

    ksp(libs.koin.ksp.compiler)
}
