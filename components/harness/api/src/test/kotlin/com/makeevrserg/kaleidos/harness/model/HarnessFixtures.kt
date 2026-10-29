package com.makeevrserg.kaleidos.harness.model

import com.makeevrserg.kaleidos.host.model.DevServerKind
import com.makeevrserg.kaleidos.host.model.GradleModule
import com.makeevrserg.kaleidos.host.model.PreviewHost
import com.makeevrserg.kaleidos.host.model.PreviewProjectStructure
import com.makeevrserg.kaleidos.host.model.PreviewProjectStructure.ModuleDependencyGraph

object HarnessFixtures {
    const val ROOT_PROJECT_PATH = "/project"

    data class ModuleDependency(val dependentPath: String, val dependencyPath: String)

    infix fun String.to(dependencyPath: String): ModuleDependency {
        return ModuleDependency(dependentPath = this, dependencyPath = dependencyPath)
    }

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

    fun sourcePathOf(fqn: String): String = fqn.replace('.', '/') + ".kt"

    fun preview(fqn: String): HarnessPreview {
        return HarnessPreview(
            fqn = fqn,
            sourcePath = sourcePathOf(fqn),
            isPrivate = false
        )
    }

    fun privatePreview(fqn: String): HarnessPreview {
        return HarnessPreview(
            fqn = fqn,
            sourcePath = sourcePathOf(fqn),
            isPrivate = true
        )
    }

    fun privatePreviewFile(fqn: String): PrivatePreviewFile {
        val packageName = fqn.substringBeforeLast('.')
        val name = fqn.substringAfterLast('.')
        return PrivatePreviewFile(
            sourcePath = sourcePathOf(fqn),
            text = """
                |package $packageName
                |
                |@Preview
                |@Composable
                |private fun $name() {}
                |
            """.trimMargin()
        )
    }

    fun previews(gradlePath: String, vararg fqns: String): ModulePreviews {
        return ModulePreviews(
            moduleDirectory = directoryOf(gradlePath),
            previews = fqns.map(::preview),
            privatePreviewFiles = emptyList()
        )
    }

    fun privatePreviews(gradlePath: String, vararg fqns: String): ModulePreviews {
        return ModulePreviews(
            moduleDirectory = directoryOf(gradlePath),
            previews = fqns.map(::privatePreview),
            privatePreviewFiles = fqns.map(::privatePreviewFile)
        )
    }

    fun structure(vararg edges: ModuleDependency): PreviewProjectStructure {
        val gradlePaths = edges.flatMap { edge -> listOf(edge.dependentPath, edge.dependencyPath) }.distinct()
        val dependencies = edges
            .groupBy(keySelector = { edge -> directoryOf(edge.dependentPath) })
            .mapValues { entry -> entry.value.map { edge -> directoryOf(edge.dependencyPath) }.toSet() }
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
