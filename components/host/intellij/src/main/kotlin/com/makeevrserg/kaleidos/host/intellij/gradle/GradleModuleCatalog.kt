package com.makeevrserg.kaleidos.host.intellij.gradle

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.task.TaskData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.makeevrserg.kaleidos.core.di.ProjectDependencies
import com.makeevrserg.kaleidos.host.api.model.GradleProject
import org.jetbrains.plugins.gradle.util.GradleConstants

/**
 * Gradle modules and their tasks as recorded by the last Gradle sync. Nothing is queried from Gradle
 * itself, so the answer is immediate but empty until the first sync of the project has finished.
 */
internal class GradleModuleCatalog(private val projectDependencies: ProjectDependencies) {

    /** The sync identifies the root project by its name and subprojects by their `:a:b` path. */
    private fun ModuleData.toGradlePath(): String {
        return if (id.startsWith(GRADLE_PATH_SEPARATOR)) id else ""
    }

    /**
     * Since 2026.2 the sync names the tasks of a subproject by their full path (`:app:kobwebStart`) and
     * copies the tasks of subprojects into every parent as "inherited" ones under the plain name, so that
     * running them from the parent works. Only the tasks the module declares itself count here, by plain
     * name, otherwise the root project looks like it owns every dev server of the build.
     */
    private fun DataNode<ModuleData>.ownTaskNames(): Set<String> {
        return ExternalSystemApiUtil.findAll(this, ProjectKeys.TASK)
            .map(DataNode<TaskData>::getData)
            .filterNot(TaskData::isInherited)
            .map { task -> task.name.substringAfterLast(GRADLE_PATH_SEPARATOR) }
            .toSet()
    }

    private fun DataNode<ModuleData>.toGradleProject(rootProjectPath: String): GradleProject {
        val taskNames = ownTaskNames()
        return GradleProject(
            gradlePath = data.toGradlePath(),
            directory = data.linkedExternalProjectPath,
            rootProjectPath = rootProjectPath,
            taskNames = taskNames
        )
    }

    fun read(): List<GradleProject> {
        return projectDependencies.projectDataManager
            .getExternalProjectsData(projectDependencies.project, GradleConstants.SYSTEM_ID)
            .flatMap { projectInfo ->
                val structure = projectInfo.externalProjectStructure ?: return@flatMap emptyList()
                ExternalSystemApiUtil.findAll(structure, ProjectKeys.MODULE)
                    .map { moduleNode -> moduleNode.toGradleProject(projectInfo.externalProjectPath) }
            }
    }

    private companion object {
        const val GRADLE_PATH_SEPARATOR = ":"
    }
}
