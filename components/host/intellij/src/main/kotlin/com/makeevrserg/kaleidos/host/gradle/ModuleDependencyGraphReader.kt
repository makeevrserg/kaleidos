package com.makeevrserg.kaleidos.host.gradle

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.makeevrserg.kaleidos.dependencies.ProjectDependencies
import com.makeevrserg.kaleidos.host.model.PreviewProjectStructure.ModuleDependencyGraph
import java.nio.file.Path

/** Reads the IDE module model. Every method must be called under a read action. */
internal class ModuleDependencyGraphReader(private val projectDependencies: ProjectDependencies) {

    /** Null for modules that were not imported from Gradle. */
    private fun gradleDirectory(module: Module): String? = ExternalSystemApiUtil.getExternalProjectPath(module)

    fun moduleDirectoryOf(filePath: String): String? {
        val file = projectDependencies.virtualFileManager.findFileByNioPath(Path.of(filePath)) ?: return null
        return projectDependencies.fileIndex.getModuleForFile(file)?.let(::gradleDirectory)
    }

    fun read(): ModuleDependencyGraph {
        val dependencies = mutableMapOf<String, MutableSet<String>>()
        projectDependencies.moduleManager.modules.forEach { module ->
            val directory = gradleDirectory(module) ?: return@forEach
            val directDependencies = dependencies.getOrPut(directory) { mutableSetOf() }
            ModuleRootManager.getInstance(module).dependencies
                .mapNotNull(::gradleDirectory)
                .filterNot { dependencyDirectory -> dependencyDirectory == directory }
                .forEach(directDependencies::add)
        }
        return ModuleDependencyGraph(dependencies)
    }
}
