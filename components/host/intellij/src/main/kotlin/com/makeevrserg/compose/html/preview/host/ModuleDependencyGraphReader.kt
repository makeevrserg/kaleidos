package com.makeevrserg.compose.html.preview.host

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.makeevrserg.compose.html.preview.dependencies.ProjectDependencies
import java.nio.file.Path

/** Reads the IDE module model. Every method must be called under a read action. */
class ModuleDependencyGraphReader(private val projectDependencies: ProjectDependencies) {

    /** Null for modules that were not imported from Gradle. */
    private fun Module.gradleDirectory(): String? = ExternalSystemApiUtil.getExternalProjectPath(this)

    fun moduleDirectoryOf(filePath: String): String? {
        val file = projectDependencies.virtualFileManager.findFileByNioPath(Path.of(filePath)) ?: return null
        return projectDependencies.fileIndex.getModuleForFile(file)?.gradleDirectory()
    }

    fun read(): ModuleDependencyGraph {
        val dependencies = mutableMapOf<String, MutableSet<String>>()
        projectDependencies.moduleManager.modules.forEach { module ->
            val directory = module.gradleDirectory() ?: return@forEach
            val directDependencies = dependencies.getOrPut(directory) { mutableSetOf() }
            ModuleRootManager.getInstance(module).dependencies
                .mapNotNull { dependency -> dependency.gradleDirectory() }
                .filterNot { dependencyDirectory -> dependencyDirectory == directory }
                .forEach(directDependencies::add)
        }
        return ModuleDependencyGraph(dependencies)
    }
}
