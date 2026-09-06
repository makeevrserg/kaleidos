package com.makeevrserg.compose.html.preview.host

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.makeevrserg.compose.html.preview.dependencies.ProjectDependencies
import org.jetbrains.plugins.gradle.util.GradleConstants

/**
 * Gradle modules and their tasks as recorded by the last Gradle sync. Nothing is queried from Gradle
 * itself, so the answer is immediate but empty until the first sync of the project has finished.
 */
class GradleModuleCatalog(private val projectDependencies: ProjectDependencies) {

    /** The sync identifies the root project by its name and subprojects by their `:a:b` path. */
    private fun ModuleData.toGradlePath(): String {
        return if (id.startsWith(GRADLE_PATH_SEPARATOR)) id else ""
    }

    private fun DataNode<ModuleData>.toGradleModule(rootProjectPath: String): GradleModule {
        val taskNames = ExternalSystemApiUtil.findAll(this, ProjectKeys.TASK)
            .map { taskNode -> taskNode.data.name }
            .toSet()
        return GradleModule(
            gradlePath = data.toGradlePath(),
            directory = data.linkedExternalProjectPath,
            rootProjectPath = rootProjectPath,
            taskNames = taskNames
        )
    }

    fun read(): List<GradleModule> {
        return projectDependencies.projectDataManager
            .getExternalProjectsData(projectDependencies.project, GradleConstants.SYSTEM_ID)
            .flatMap { projectInfo ->
                val structure = projectInfo.externalProjectStructure ?: return@flatMap emptyList()
                ExternalSystemApiUtil.findAll(structure, ProjectKeys.MODULE)
                    .map { moduleNode -> moduleNode.toGradleModule(projectInfo.externalProjectPath) }
            }
    }

    private companion object {
        const val GRADLE_PATH_SEPARATOR = ":"
    }
}
