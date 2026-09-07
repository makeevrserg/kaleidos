package com.makeevrserg.kaleidos.server

import com.makeevrserg.kaleidos.host.DevServerKind
import com.makeevrserg.kaleidos.server.PreviewHostFixtures.host
import com.makeevrserg.kaleidos.server.PreviewHostFixtures.options
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DevServerLaunchConfigTest {

    @Test
    fun GIVEN_subproject_host_WHEN_forHost_THEN_task_is_qualified_with_gradle_path() {
        val config = DevServerLaunchConfig.forHost(
            host = host(":instances:web-preview", DevServerKind.WEBPACK),
            options = options(devServerPort = 8301)
        )

        assertEquals(":instances:web-preview:jsBrowserDevelopmentRun", config.qualifiedTaskName)
        assertEquals("/project", config.rootProjectPath)
        assertEquals("--continuous", config.arguments)
    }

    @Test
    fun GIVEN_root_project_host_WHEN_forHost_THEN_task_is_addressed_as_root_task() {
        val config = DevServerLaunchConfig.forHost(host("", DevServerKind.KOBWEB), options())

        assertEquals(":kobwebStart", config.qualifiedTaskName)
    }

    @Test
    fun GIVEN_generated_sources_WHEN_forHost_THEN_the_run_gets_the_init_script_and_the_task_arguments() {
        val config = DevServerLaunchConfig.forHost(host(":instances:web-app", DevServerKind.KOBWEB), options())

        assertEquals("--init-script \"${PreviewHostFixtures.INIT_SCRIPT_PATH}\" -t", config.scriptParameters)
        assertTrue(config.scriptParameters.endsWith(config.arguments))
    }
}
