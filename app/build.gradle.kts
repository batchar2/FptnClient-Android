import com.android.build.gradle.internal.tasks.factory.dependsOn
import java.io.FileInputStream
import java.io.InputStream
import java.util.Properties
import kotlin.concurrent.thread

plugins {
    id("pvnclient.android.application")
    alias(libs.plugins.protobuf)
}

fun readStreamAsync(stream: InputStream, label: String) = thread {
    stream.bufferedReader().useLines { lines ->
        lines.forEach { println("[$label] $it") }
    }
}

//tasks.register("conanInstall") {
//    val conanExecutable = "conan" // define the path to your conan installation
//    val buildDir = file("app/build")
//    buildDir.mkdirs()
//
//    val buildTypes = listOf("Debug", "Release")
////    val architectures = listOf("armv7", "armv8") // , "x86", "x86_64")
//    val architectures = listOf("armv8") // , "x86", "x86_64")
//    doLast {
//        buildTypes.forEach { buildType ->
//            architectures.forEach { arch ->
//                //  add fptn lib
//                 var fptnCmd = "$conanExecutable export ../../src/main/cpp/.conan/recipes/fptn --name=fptn --version=0.0.0 --user=local --channel=local "
////                var fptnCmd =
////                    "$conanExecutable export ../../src/main/cpp/.conan/recipes/fptn/src/fptn-protocol-lib --name=fptn-lib --version=0.0.0 --user=local --channel=local "
//                val fptnProc = ProcessBuilder(fptnCmd.split(" "))
//                    .directory(buildDir)
//                    .start()
//                println("Project directory: ${project.projectDir}")
////                println("!!!!!!!!!!!!!!!! Current working directory: ${File(".").absolutePath}")
////                throw Exception("Execution failed!")
////                // install conan
//                val cmd = "$conanExecutable install ../../src/main/cpp --profile android-studio " +
//                        "-s build_type=$buildType -s arch=$arch --build missing " +
//                        "-c tools.cmake.cmake_layout:build_folder_vars=['settings.arch']"
//                // /Users/stanislav/Apps/My/PVNClient/app
////                val cmd = "$conanExecutable install ${project.projectDir}/src/main/cpp --profile android-studio " +
////                        "-s build_type=$buildType -s arch=$arch --build missing " +
////                        "-c tools.cmake.cmake_layout:build_folder_vars=['settings.arch']"
//                println("RUN COMMAND: =============================================================")
//                println(cmd)
//                val proc = ProcessBuilder(cmd.split(" "))
//                    .directory(buildDir)
//                    .start()
//
//                val exportOut = readStreamAsync(proc.inputStream, "stdout")
//                val exportErr = readStreamAsync(proc.errorStream, "stderr")
//                proc.waitFor()
//
//                exportOut.join()
//                exportErr.join()
//                if (proc.exitValue() != 0) {
//                    throw Exception("Execution failed!")
//                }
//            }
//        }
//    }
//}

// GOOD
//tasks.register("conanInstall") {
//    val conanExecutable = "conan"
//    val buildDir = file("app/build")
//    buildDir.mkdirs()
//
//    val buildTypes = listOf("Debug", "Release")
//    val architectures = listOf("armv8")
//
//    doLast {
//        buildTypes.forEach { buildType ->
//            architectures.forEach { arch ->
//                // Export fptn library
//                val fptnCmd = listOf(
//                    conanExecutable,
//                    "export",
//                    "../../src/main/cpp/.conan/recipes/fptn",
//                    "--name=fptn",
//                    "--version=0.0.0",
//                    "--user=local",
//                    "--channel=local"
//                )
//
//                println("\n=== Exporting fptn library ===")
//                println("Command: ${fptnCmd.joinToString(" ")}")
//
//                val fptnProc = ProcessBuilder(fptnCmd)
//                    .directory(buildDir)
//                    .redirectErrorStream(true)
//                    .start()
//
//                fptnProc.inputStream.bufferedReader().use { reader ->
//                    reader.lines().forEach { line ->
//                        println("[fptn export] $line")
//                    }
//                }
//
//                val fptnExitCode = fptnProc.waitFor()
//                if (fptnExitCode != 0) {
//                    throw GradleException("Failed to export fptn library (exit code $fptnExitCode)")
//                }
//
//                // Install dependencies
//                val installCmd = listOf(
//                    conanExecutable,
//                    "install",
//                    "../../src/main/cpp",
//                    "--profile", "android-studio",
//                    "-s", "build_type=$buildType",
//                    "-s", "arch=$arch",
//                    "--build", "missing",
//                    "-c", "tools.cmake.cmake_layout:build_folder_vars=['settings.arch']"
//                )
//
//                println("\n=== Installing dependencies for $buildType/$arch ===")
//                println("Command: ${installCmd.joinToString(" ")}")
//
//                val installProc = ProcessBuilder(installCmd)
//                    .directory(buildDir)
//                    .redirectErrorStream(true)
//                    .start()
//
//                installProc.inputStream.bufferedReader().use { reader ->
//                    reader.lines().forEach { line ->
//                        println("[conan install] $line")
//                    }
//                }
//
//                val installExitCode = installProc.waitFor()
//                if (installExitCode != 0) {
//                    throw GradleException("Conan install failed for $buildType/$arch (exit code $installExitCode)")
//                }
//
//                println("\n✔ Successfully installed dependencies for $buildType/$arch\n")
//            }
//        }
//    }
//}
//tasks.register("conanInstall") {
//    val conanExecutable = "conan"
//    // Use Android Studio's expected build directory
//    val buildDir = file("${project.buildDir}/../.cxx/cmake/arm64-v8a")
//    buildDir.mkdirs()
//
//    val buildType = "Debug" // Build only Debug for development
//    val arch = "armv8"
//
//    doLast {
//        // Export fptn library
////        val fptnCmd = listOf(
////            conanExecutable,
////            "export",
////            "../../src/main/cpp/.conan/recipes/fptn",
////            "--name=fptn",
////            "--version=0.0.0",
////            "--user=local",
////            "--channel=local"
////        )
////
////        println("\n=== Exporting fptn library ===")
////        val fptnExitCode = ProcessBuilder(fptnCmd)
////            .directory(buildDir)
////            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
////            .redirectError(ProcessBuilder.Redirect.INHERIT)
////            .start()
////            .waitFor()
////
////        if (fptnExitCode != 0) {
////            throw GradleException("Failed to export fptn library")
////        }
//
//        // Install dependencies
//        val installCmd = listOf(
//            conanExecutable,
//            "install",
//            "../../src/main/cpp",
//            "--profile", "android-studio",
//            "-s", "build_type=$buildType",
//            "-s", "arch=$arch",
//            "--build", "missing",
//            "-c", "tools.cmake.cmake_layout:build_folder_vars=['settings.arch']",
//            "--output-folder=${buildDir.absolutePath}"
//        )
//
//        println("\n=== Installing dependencies ===")
//        val installExitCode = ProcessBuilder(installCmd)
//            .directory(buildDir)
//            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
//            .redirectError(ProcessBuilder.Redirect.INHERIT)
//            .start()
//            .waitFor()
//
//        if (installExitCode != 0) {
//            throw GradleException("Conan install failed")
//        }
//
//        // Copy generated files to where Android Studio expects them
//        copy {
//            from("${buildDir.absolutePath}/armv8/$buildType/generators")
//            into("${buildDir.absolutePath}/generators")
//        }
//
//        println("\n✔ Successfully installed dependencies\n")
//    }
//}

tasks.register("conanInstall") {
    val conanExecutable = "conan"
    val buildDir = file("app/build")
    buildDir.mkdirs()

    val buildTypes = listOf("Debug", "Release")
    val architectures = listOf("armv8")

    doLast {
        buildTypes.forEach { buildType ->
            architectures.forEach { arch ->
                // Export fptn library
                val fptnCmd = listOf(
                    conanExecutable,
                    "export",
                    "../../src/main/cpp/.conan/recipes/fptn",
                    "--name=fptn",
                    "--version=0.0.0",
                    "--user=local",
                    "--channel=local"
                )

                println("\n=== Exporting fptn library ===")
                println("Command: ${fptnCmd.joinToString(" ")}")

                val fptnProc = ProcessBuilder(fptnCmd)
                    .directory(buildDir)
                    .redirectErrorStream(true)
                    .start()

                fptnProc.inputStream.bufferedReader().use { reader ->
                    reader.lines().forEach { line ->
                        println("[fptn export] $line")
                    }
                }

                val fptnExitCode = fptnProc.waitFor()
                if (fptnExitCode != 0) {
                    throw GradleException("Failed to export fptn library (exit code $fptnExitCode)")
                }

                // Install dependencies
                val installCmd = listOf(
                    conanExecutable,
                    "install",
                    "../../src/main/cpp",
                    "--profile", "android-studio",
                    "-s", "build_type=$buildType",
                    "-s", "arch=$arch",
                    "--build", "missing",
                    "-c", "tools.cmake.cmake_layout:build_folder_vars=['settings.arch']"
                )

                println("\n=== Installing dependencies for $buildType/$arch ===")
                println("Command: ${installCmd.joinToString(" ")}")

                val installProc = ProcessBuilder(installCmd)
                    .directory(buildDir)
                    .redirectErrorStream(true)
                    .start()

                installProc.inputStream.bufferedReader().use { reader ->
                    reader.lines().forEach { line ->
                        println("[conan install] $line")
                    }
                }

                val installExitCode = installProc.waitFor()
                if (installExitCode != 0) {
                    throw GradleException("Conan install failed for $buildType/$arch (exit code $installExitCode)")
                }

                println("\n✔ Successfully installed dependencies for $buildType/$arch\n")
            }
        }
    }
}


tasks.named("preBuild").configure {
    dependsOn("conanInstall")
}

fun findProtocPath(): String {
    val process = ProcessBuilder("which", "protoc")
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()

    return process.inputStream.bufferedReader().use { it.readText() }.trim()
}
val protocPath = findProtocPath()

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
                arguments(
                    "-DCMAKE_TOOLCHAIN_FILE=${projectDir}/src/main/cpp/conan_android_toolchain.cmake"
                    //,
                    //"-DProtobuf_PROTOC_EXECUTABLE=$protocPath"
                )
            }
        }
        ndk {
            abiFilters.add("arm64-v8a")
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
