package com.makeevrserg.compose.html.preview.harness

import com.makeevrserg.compose.html.preview.host.DevServerKind
import com.makeevrserg.compose.html.preview.host.GradleModule
import com.makeevrserg.compose.html.preview.host.ModuleDependencyGraph
import com.makeevrserg.compose.html.preview.host.PreviewHost
import com.makeevrserg.compose.html.preview.host.PreviewProjectStructure

object HarnessFixtures {
    const val ROOT_PROJECT_PATH = "/project"

    fun directoryOf(gradlePath: String): String = ROOT_PROJECT_PATH + gradlePath.replace(':', '/')

    fun module(gradlePath: String): GradleModule {
        return GradleModule(
            gradlePath = gradlePath,
            directory = directoryOf(gradlePath),
            rootProjectPath = ROOT_PROJECT_PATH,
            taskNames = emptySet()
        )
    }

    fun host(gradlePath: String, kind: DevServerKind): PreviewHost {
        return PreviewHost(
            gradlePath = gradlePath,
            directory = directoryOf(gradlePath),
            rootProjectPath = ROOT_PROJECT_PATH,
            kind = kind
        )
    }

    fun previews(gradlePath: String, vararg fqns: String): ModulePreviews {
        return ModulePreviews(
            moduleDirectory = directoryOf(gradlePath),
            previews = fqns.map(::HarnessPreview)
        )
    }

    fun structure(vararg edges: Pair<String, String>): PreviewProjectStructure {
        val gradlePaths = edges.flatMap { edge -> listOf(edge.first, edge.second) }.distinct()
        val dependencies = edges
            .groupBy(keySelector = { edge -> directoryOf(edge.first) })
            .mapValues { entry -> entry.value.map { edge -> directoryOf(edge.second) }.toSet() }
        return PreviewProjectStructure(
            modules = gradlePaths.map(::module),
            dependencies = ModuleDependencyGraph(dependencies)
        )
    }

    fun structureOf(vararg gradlePaths: String): PreviewProjectStructure {
        return PreviewProjectStructure(
            modules = gradlePaths.map(::module),
            dependencies = ModuleDependencyGraph(emptyMap())
        )
    }
}
