plugins {
    `kotlin-dsl`
}

buildscript {
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
    }
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "org.fptn.vpn.application"
            implementationClass = "org.fptn.vpn.gradle.AndroidApplicationConventionPlugin"
        }
        register("androidApplicationCompose") {
            id = "org.fptn.vpn.application.compose"
            implementationClass = "org.fptn.vpn.gradle.AndroidApplicationComposeConventionPlugin"
        }
        register("androidLibrary") {
            id = "org.fptn.vpn.library.android"
            implementationClass = "org.fptn.vpn.gradle.AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "org.fptn.vpn.library.android.compose"
            implementationClass = "org.fptn.vpn.gradle.AndroidLibraryComposeConventionPlugin"
        }
        register("kotlinLibrary") {
            id = "org.fptn.vpn.library.kotlin"
            implementationClass = "org.fptn.vpn.gradle.KotlinLibraryConventionPlugin"
        }
    }
}

dependencies {
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
    implementation(libs.detekt)
    implementation(libs.android.gradle.plugin)
    implementation(libs.guava)
    implementation(libs.kotlin.gradle.plugin)
}
