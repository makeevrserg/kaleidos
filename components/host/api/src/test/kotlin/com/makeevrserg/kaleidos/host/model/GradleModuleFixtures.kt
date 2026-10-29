package com.makeevrserg.kaleidos.host.model

import com.makeevrserg.kaleidos.host.model.PreviewProjectStructure.ModuleDependencyGraph

object GradleModuleFixtures {
    const val ROOT_PROJECT_PATH = "/project"

    data class ModuleDependency(val dependentPath: String, val dependencyPath: String)

    infix fun String.to(dependencyPath: String): ModuleDependency {
        return ModuleDependency(dependentPath = this, dependencyPath = dependencyPath)
    }

    fun module(gradlePath: String, taskNames: Set<String> = emptySet()): GradleModule {
        return GradleModule(
            gradlePath = gradlePath,
            directory = ROOT_PROJECT_PATH + gradlePath.replace(':', '/'),
            rootProjectPath = ROOT_PROJECT_PATH,
            taskNames = taskNames
        )
    }

    fun jsBrowserModule(gradlePath: String): GradleModule = module(gradlePath, setOf("jsBrowserTest"))

    fun kobwebApplication(gradlePath: String): GradleModule {
        return module(gradlePath, setOf("jsBrowserTest", "kobwebStart"))
    }

    fun kobwebLibrary(gradlePath: String): GradleModule {
        return module(gradlePath, setOf("jsBrowserTest", "kobwebGenerateLibraryMetadata"))
    }

    /** Direct dependencies by Gradle path, converted to the directory keys the graph uses. */
    fun graph(vararg edges: ModuleDependency): ModuleDependencyGraph {
        val dependencies = edges
            .groupBy(keySelector = { edge -> module(edge.dependentPath).directory })
            .mapValues { entry -> entry.value.map { edge -> module(edge.dependencyPath).directory }.toSet() }
        return ModuleDependencyGraph(dependencies)
    }

    fun structure(modules: List<GradleModule>, vararg edges: ModuleDependency): PreviewProjectStructure {
        return PreviewProjectStructure(modules = modules, dependencies = graph(*edges))
    }
}
