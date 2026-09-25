package com.makeevrserg.kaleidos.ui.console

import com.makeevrserg.kaleidos.server.DevServerLaunchConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DevServerConsoleTextsTest {
    private val texts = DevServerConsoleTexts()

    @Test
    fun GIVEN_run_WHEN_it_starts_THEN_the_console_shows_the_task_with_its_arguments_on_a_line_of_its_own() {
        val config = DevServerLaunchConfig(
            rootProjectPath = "/project",
            gradlePath = ":instances:web-app",
            taskName = "kobwebStart",
            arguments = "-t",
            initScriptPath = "/project/instances/web-app/build/kaleidos/kaleidos.init.gradle"
        )

        assertEquals(
            """
                |> :instances:web-app:kobwebStart --init-script "/project/instances/web-app/build/kaleidos/kaleidos.init.gradle" -t
                |
            """.trimMargin(),
            texts.runStarted(config)
        )
    }

    @Test
    fun GIVEN_adopted_server_WHEN_shown_THEN_the_console_names_its_address_and_the_way_to_run_it_here() {
        val text = texts.adopted("http://localhost:8080")

        assertTrue(text.contains("http://localhost:8080"), text)
        assertTrue(text.contains("Restart Dev Server"), text)
        assertTrue(text.endsWith("\n"), text)
    }
}
