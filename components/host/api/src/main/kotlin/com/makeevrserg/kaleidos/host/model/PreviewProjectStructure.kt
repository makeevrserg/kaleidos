package com.makeevrserg.kaleidos.host.model

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

    data class ModuleDependencyGraph(val dependencies: Map<String, Set<String>>)
}
