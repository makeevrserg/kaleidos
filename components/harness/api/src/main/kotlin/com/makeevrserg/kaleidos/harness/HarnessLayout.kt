package com.makeevrserg.kaleidos.harness

import java.nio.file.Path

/**
 * Everything generated lives in the build directory of the module it belongs to: ignored by version
 * control, removed by `clean`, and never mixed into the sources of the project.
 */
class HarnessLayout {

    fun root(moduleDirectory: String): Path = Path.of(moduleDirectory, BUILD_DIRECTORY, GENERATED_DIRECTORY)

    fun kotlinRoot(moduleDirectory: String): Path = root(moduleDirectory).resolve(KOTLIN_DIRECTORY)

    fun resourcesRoot(moduleDirectory: String): Path = root(moduleDirectory).resolve(RESOURCES_DIRECTORY)

    fun initScript(hostDirectory: String): Path = root(hostDirectory).resolve(INIT_SCRIPT_NAME)

    companion object {
        const val KOTLIN_DIRECTORY = "kotlin"
        const val RESOURCES_DIRECTORY = "resources"
        const val INIT_SCRIPT_NAME = "kaleidos.init.gradle"
        private const val BUILD_DIRECTORY = "build"
        private const val GENERATED_DIRECTORY = "kaleidos"
    }
}
