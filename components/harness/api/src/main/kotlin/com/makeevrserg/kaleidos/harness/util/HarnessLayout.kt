package com.makeevrserg.kaleidos.harness.util

import java.nio.file.Path

internal const val KOTLIN_DIRECTORY = "kotlin"
internal const val RESOURCES_DIRECTORY = "resources"
internal const val INIT_SCRIPT_NAME = "kaleidos.init.gradle"
private const val BUILD_DIRECTORY = "build"
private const val GENERATED_DIRECTORY = "kaleidos"

internal fun harnessRoot(moduleDirectory: String): Path {
    return Path.of(
        moduleDirectory,
        BUILD_DIRECTORY,
        GENERATED_DIRECTORY
    )
}

internal fun harnessKotlinRoot(moduleDirectory: String): Path = harnessRoot(moduleDirectory).resolve(KOTLIN_DIRECTORY)

internal fun harnessResourcesRoot(moduleDirectory: String): Path {
    return harnessRoot(moduleDirectory).resolve(RESOURCES_DIRECTORY)
}

internal fun harnessInitScript(hostDirectory: String): Path = harnessRoot(hostDirectory).resolve(INIT_SCRIPT_NAME)
