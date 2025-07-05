plugins {
    id("org.fptn.vpn.library.kotlin")
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.koin.core)
    implementation(libs.kotlinx.coroutines.core)
}
