package com.makeevrserg.kaleidos.host.util

import com.makeevrserg.kaleidos.host.model.PreviewProjectStructure

fun PreviewProjectStructure.gradlePathOf(moduleDirectory: String): String? {
    return modules.firstOrNull { module -> module.directory == moduleDirectory }?.gradlePath
}

/** True for the module itself and for every module it reaches through its dependencies. */
fun PreviewProjectStructure.reaches(moduleDirectory: String, dependencyDirectory: String): Boolean {
    return (moduleDirectory == dependencyDirectory)
        .or(dependencies.dependsOn(moduleDirectory, dependencyDirectory))
}
