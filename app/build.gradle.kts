import com.android.build.gradle.internal.tasks.factory.dependsOn
import java.io.FileInputStream
import java.util.Properties

plugins {
    id("pvnclient.android.application")
    alias(libs.plugins.protobuf)
}

tasks.register("conanInstall") {
    val conanExecutable = "conan" // define the path to your conan installation
    val buildDir = file("app/build")
    buildDir.mkdirs()

    val buildTypes = listOf("Debug", "Release")
    val architectures = listOf("armv7", "armv8", "x86", "x86_64")
    doLast {
        buildTypes.forEach { buildType ->
            architectures.forEach { arch ->
                //  add fptn lib
                var fptnCmd = "$conanExecutable export ../../src/main/cpp/.conan/recipes/fptn --name=fptn --version=0.0.0 --user=local --channel=local "
                val fptnProc = ProcessBuilder(fptnCmd.split(" "))
                    .directory(buildDir)
                    .start()

                // install conan
                val cmd = "$conanExecutable install ../../src/main/cpp --profile android-studio " +
                        "-s build_type=$buildType -s arch=$arch --build missing " +
                        "-c tools.cmake.cmake_layout:build_folder_vars=['settings.arch']"
                val proc = ProcessBuilder(cmd.split(" "))
                    .directory(buildDir)
                    .start()

                val result = proc.inputStream.bufferedReader().readText()
                val errors = proc.errorStream.bufferedReader().readText()

                proc.waitFor()

                if (proc.exitValue() != 0) {
                    throw Exception("Execution failed! Output: $result Error: $errors")
                }
                println(result)
                if (errors.isNotBlank()) {
                    println("Errors: $errors")
                }
            }
        }
    }
}
tasks.named("preBuild").configure {
    dependsOn("conanInstall")
}

android {
    namespace = "org.fptn.vpn"
    compileSdk = rootProject.extra.get("compileSdkVersion") as Int

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
                cppFlags += "-std=c++17"
                arguments("-DCMAKE_TOOLCHAIN_FILE=conan_android_toolchain.cmake")
            }
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
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    project.tasks.preBuild.dependsOn("conanInstall")
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
    implementation(libs.conscrypt.android)
    implementation(libs.guava)
    implementation(libs.ipaddress)
    implementation(libs.material)
    implementation(libs.okhttp)
    implementation(libs.protobuf.javalite)
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
