package com.makeevrserg.compose.html.preview.host

/**
 * The build as the last Gradle sync recorded it: every module with its tasks, and who depends on whom.
 *
 * @param dependencies keyed by module directory, the identity shared by the IDE modules of one Gradle module
 */
data class PreviewProjectStructure(
    val modules: List<GradleModule>,
    val dependencies: ModuleDependencyGraph
) {
    val isImported: Boolean
        get() = modules.isNotEmpty()

    fun gradlePathOf(moduleDirectory: String): String? {
        return modules.firstOrNull { module -> module.directory == moduleDirectory }?.gradlePath
    }

    /** True for the module itself and for every module it reaches through its dependencies. */
    fun reaches(moduleDirectory: String, dependencyDirectory: String): Boolean {
        return moduleDirectory == dependencyDirectory ||
            dependencies.dependsOn(moduleDirectory, dependencyDirectory)
    }
}
