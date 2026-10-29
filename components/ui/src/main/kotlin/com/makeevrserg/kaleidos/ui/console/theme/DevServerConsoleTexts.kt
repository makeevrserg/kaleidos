package com.makeevrserg.kaleidos.ui.console.theme

import com.makeevrserg.kaleidos.server.model.DevServerLaunchConfig

class DevServerConsoleTexts {

    fun runStarted(config: DevServerLaunchConfig): String {
        return "> ${config.qualifiedTaskName} ${config.scriptParameters}\n"
    }

    fun adopted(baseUrl: String): String {
        return "Showing the dev server that already runs at $baseUrl. It was not started by this run, so its " +
            "output is wherever it was started. Restart Dev Server runs it here.\n"
    }
}
