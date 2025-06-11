import java.io.FileInputStream
import java.util.Properties
import java.io.InputStream
import kotlin.concurrent.thread

plugins {
    id("pvnclient.android.application")
}

android {
    namespace = "org.fptn.vpn"
    compileSdk = rootProject.extra.get("compileSdkVersion") as Int
    // ndkVersion = "27.2.12479018"

    signingConfigs {
        create("release") {
            val keystorePropertiesFile: File = rootProject.file("keystore.properties")
            val keystoreProperties = Properties()
            if (keystorePropertiesFile.exists()) {
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = file(keystoreProperties["storeFile"]!!)
                storePassword = keystoreProperties["storePassword"] as String
            } else {
                println(
                    "Warning: keystore.properties file not found. " +
                            "Release signing configuration will not be applied.",
                )
            }
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
            abiFilters += listOf("arm64-v8a")
//            abiFilters += listOf("x86_64", "arm64-v8a")
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


fun readStreamAsync(stream: InputStream, label: String) = thread {
    stream.bufferedReader().useLines { lines ->
        lines.forEach { println("[$label] $it") }
    }
}

task("conanInstall") {
    val conanExecutable = "conan" // define the path to your conan installation
    val buildDir = file("build").apply { mkdirs() }

    val absoluteBuildDirPath = buildDir.absolutePath
    println("Build directory: $absoluteBuildDirPath")

    listOf("Debug", "Release", "RelWithDebInfo").forEach { buildType ->
        listOf("armv8", "x86_64").forEach { arch ->
            val cmd =
                "$conanExecutable install " +
                        "../src/main/cpp --profile android-studio -s build_type=$buildType -s arch=$arch " +
                        "--build missing -c tools.cmake.cmake_layout:build_folder_vars=['settings.arch']"
            println(">> $cmd")
            val sout = StringBuilder()
            val serr = StringBuilder()
            val proc = Runtime.getRuntime().exec(cmd, null, buildDir)

            val exportOut = readStreamAsync(proc.inputStream, "stdout")
            val exportErr = readStreamAsync(proc.errorStream, "stderr")

            val exitCode = proc.waitFor()
            println("$sout $serr")

            if (exitCode != 0) {
                throw Exception("out> $sout err> $serr\nCommand: $cmd")
            }
        }
    }
}
