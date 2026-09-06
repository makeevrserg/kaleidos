package com.makeevrserg.compose.html.preview.server

import com.makeevrserg.compose.html.preview.host.DevServerKind
import com.makeevrserg.compose.html.preview.server.PreviewHostFixtures.host
import kotlin.test.Test
import kotlin.test.assertEquals

class DevServerLaunchConfigTest {

    @Test
    fun GIVEN_subproject_host_WHEN_forHost_THEN_task_is_qualified_with_gradle_path() {
        val config = DevServerLaunchConfig.forHost(
            host = host(":instances:web-preview", DevServerKind.WEBPACK),
            taskName = "jsBrowserDevelopmentRun",
            arguments = "--continuous"
        )

        assertEquals(":instances:web-preview:jsBrowserDevelopmentRun", config.qualifiedTaskName)
        assertEquals("/project", config.rootProjectPath)
        assertEquals("--continuous", config.arguments)
    }

    @Test
    fun GIVEN_root_project_host_WHEN_forHost_THEN_task_is_addressed_as_root_task() {
        val config = DevServerLaunchConfig.forHost(
            host = host("", DevServerKind.KOBWEB),
            taskName = "kobwebStart",
            arguments = "-t"
        )

        assertEquals(":kobwebStart", config.qualifiedTaskName)
    }
}
