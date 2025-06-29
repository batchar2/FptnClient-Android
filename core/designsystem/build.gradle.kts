import org.fptn.vpn.gradle.extensions.ksp

plugins {
    id("org.fptn.vpn.library.android")

    
}

android {
    namespace = "org.fptn.vpn.core.designsystem"
}

dependencies {
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)

    debugImplementation(libs.androidx.compose.ui.tooling)

    
}
