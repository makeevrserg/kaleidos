package com.makeevrserg.compose.html.preview.startup

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.makeevrserg.compose.html.preview.service.PreviewProjectService

/**
 * Instantiates the project service on project open so editor and tool window tracking start before
 * the user interacts with the plugin. Light services are lazy otherwise.
 */
class PreviewStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        project.service<PreviewProjectService>()
    }
}
