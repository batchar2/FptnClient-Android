plugins {
    id("pvnclient.android.library.android")
}

android {
    namespace = "com.filantrop.pvnclient.auth.data"
}

dependencies {

    implementation(project(":auth:domain"))
}