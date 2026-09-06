package com.makeevrserg.compose.html.preview.host

object GradleModuleFixtures {
    const val ROOT_PROJECT_PATH = "/project"

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
    fun graph(vararg edges: Pair<String, String>): ModuleDependencyGraph {
        val dependencies = edges
            .groupBy(keySelector = { edge -> module(edge.first).directory })
            .mapValues { entry -> entry.value.map { edge -> module(edge.second).directory }.toSet() }
        return ModuleDependencyGraph(dependencies)
    }

    fun structure(modules: List<GradleModule>, vararg edges: Pair<String, String>): PreviewProjectStructure {
        return PreviewProjectStructure(modules = modules, dependencies = graph(*edges))
    }
}
