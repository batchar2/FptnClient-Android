/*
 * FPTN Android Client
 * Copyright (C) 2026  Skokov Stanislav, Enin Sergey
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Website: https://fptn.org
 */

import org.fptn.vpn.gradle.extensions.configureKotlin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class KotlinLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("kotlin")
                apply("com.autonomousapps.dependency-analysis")
            }
            // Pin the JVM target instead of letting it silently follow whatever JDK Gradle
            // happens to run on — otherwise this module's class files can end up newer than
            // what :app (pinned to Java 17) can read, e.g. "class file has wrong version".
            // Both the Java toolchain (compileJava, even with no .java sources the `kotlin`
            // plugin still applies the Java plugin) and the Kotlin compiler's own jvmTarget
            // need pinning, or Gradle rejects the mismatch between the two.
            extensions.configure<JavaPluginExtension> {
                toolchain {
                    languageVersion.set(JavaLanguageVersion.of(17))
                }
            }
            configureKotlin()
            dependencies {
            }
        }
    }
}
