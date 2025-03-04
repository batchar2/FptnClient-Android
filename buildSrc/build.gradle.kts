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
            id = "pvnclient.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("kotlinLibrary") {
            id = "pvnclient.android.library.kotlin"
            implementationClass = "KotlinLibraryConventionPlugin"
        }
    }
}

dependencies {
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
    implementation(libs.detekt)
    implementation(libs.android.gradle.plugin)
    implementation(libs.guava)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.ksp.gradle.plugin)
    implementation(libs.conscrypt)
}
