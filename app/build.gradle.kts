plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.protobuf)
}

android {
    namespace = "com.filantrop.pvnclient"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.filantrop.pvnclient"
        minSdk = 26
        targetSdkVersion(rootProject.extra["defaultTargetSdkVersion"] as Int)
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.monitor)
    implementation(libs.androidx.activity)
    implementation(libs.protobuf.javalite)
    implementation(libs.okhttp)

    // add lombok
    compileOnly(libs.lombock)
    annotationProcessor(libs.lombock)

    //adding ORM Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.guava)
    annotationProcessor(libs.androidx.room.compiler)
    testImplementation(libs.androidx.room.testing)

    implementation(libs.guava)

    // To use CallbackToFutureAdapter
    implementation(libs.androidx.concurrent.futures)


    //for tests (but we have no tests! yet?)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

protobuf {
    protoc {
        artifact = libs.protoc.get().toString()
    }
    generateProtoTasks {
        all().forEach {
            it.builtins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}