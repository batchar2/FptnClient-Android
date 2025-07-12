import java.io.FileInputStream
import java.io.InputStream
import java.util.Properties
import kotlin.concurrent.thread

plugins {
    id("pvnclient.android.application")
}

android {
    namespace = "org.fptn.vpn"
    compileSdk = rootProject.extra.get("compileSdkVersion") as Int
    ndkVersion = "28.1.13356709"

    signingConfigs {
        create("release") {
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
            storeFile = file(System.getenv("KEYSTORE_PATH"))
            storePassword = System.getenv("STORE_PASSWORD")
        }
    }

    defaultConfig {
        applicationId = "org.fptn.vpn"
        val versionMajor: Int by rootProject.extra
        val versionMinor: Int by rootProject.extra
        val versionPatch: Int by rootProject.extra
        val versionBuild: Int by rootProject.extra
        versionCode =
            1000 * (1000 * versionMajor + 100 * versionMinor + versionPatch) + versionBuild
        versionName = "$versionMajor.$versionMinor.$versionPatch.$versionBuild"

        minSdk = rootProject.extra.get("minSdkVersion") as Int
        targetSdk = rootProject.extra.get("targetSdkVersion") as Int

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables.useSupportLibrary = true

        externalNativeBuild {
            cmake {
                cppFlags("-v")
                arguments("-DCMAKE_TOOLCHAIN_FILE=conan_android_toolchain.cmake")
            }
        }

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }

    externalNativeBuild {
        cmake {
            // version = "3.31.6"
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":vpnclient"))
    implementation(libs.androidx.activity)
    implementation(libs.androidx.appcompat)
    // To use CallbackToFutureAdapter
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.monitor)
    implementation(libs.androidx.room.guava)
    implementation(libs.androidx.room.runtime)
    implementation(libs.guava)
    implementation(libs.ipaddress)
    implementation(libs.jackson.databind)
    implementation(libs.material)
    implementation(libs.zxing)

    compileOnly(libs.lombock)

    annotationProcessor(libs.androidx.room.compiler)
    annotationProcessor(libs.lombock)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
}
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

fun readStreamAsync(
    stream: InputStream,
    label: String,
) = thread {
    stream.bufferedReader().useLines { lines ->
        lines.forEach { println("[$label] $it") }
    }
}

tasks.register("conanInstall") {
    group = "c++"
    doLast {
        val buildDir = file("$buildDir/conan").apply { mkdirs() }
        listOf("Debug", "Release", "RelWithDebInfo").forEach { buildType ->
            listOf("armv8", "armv7").forEach { arch ->
                exec {
                    workingDir = buildDir
                    commandLine(
                        "conan",
                        "install",
                        "$projectDir/src/main/cpp",
                        "--profile",
                        "$rootDir/conan/profiles/android-studio",
                        "-s",
                        "build_type=$buildType",
                        "-s",
                        "arch=$arch",
                        "--build",
                        "missing",
                        "-c",
                        "tools.cmake.cmake_layout:build_folder_vars=['settings.arch']",
                    )
                }
            }
        }
    }
}
tasks.named("preBuild") { dependsOn("conanInstall") }
