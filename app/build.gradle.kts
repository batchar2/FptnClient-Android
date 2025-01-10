import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    id("com.android.application")
    alias(libs.plugins.protobuf)
}

android {
    namespace = "com.filantrop.pvnclient"
    compileSdk = rootProject.extra.get("compileSdkVersion") as Int

    val dateTimeString: String = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now())

    defaultConfig {
        applicationId = "com.filantrop.pvnclient"
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

    applicationVariants.all {
        outputs.all {
            // Define the new APK file name
            val newApkName = "fptn-client-$dateTimeString.apk"
            println("SOI")

            // Rename the output file
            outputFile.renameTo(File(outputFile.parent, newApkName))
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":vpnclient"))
    implementation(libs.androidx.activity)
    implementation(libs.androidx.appcompat)
    // To use CallbackToFutureAdapter
    implementation(libs.androidx.concurrent.futures)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.monitor)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.room.guava)
    // adding ORM Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.guava)
    implementation(libs.ipaddress)
    implementation(libs.material)
    implementation(libs.okhttp)
    implementation(libs.protobuf.javalite)

    // add lombok
    compileOnly(libs.lombock)

    annotationProcessor(libs.androidx.room.compiler)
    annotationProcessor(libs.lombock)

    testImplementation(libs.androidx.room.testing)
    // for tests (but we have no tests! yet?)
    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
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
