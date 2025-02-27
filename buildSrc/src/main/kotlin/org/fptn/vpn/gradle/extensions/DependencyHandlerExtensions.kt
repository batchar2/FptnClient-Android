package org.fptn.vpn.gradle.extensions

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler

val Project.buildLibs get() = extensions.getByName("libs") as LibrariesForLibs

fun DependencyHandler.implementation(dependencyNotation: Any) = add("implementation", dependencyNotation)

fun DependencyHandler.ksp(dependencyNotation: Any) = add("ksp", dependencyNotation)
