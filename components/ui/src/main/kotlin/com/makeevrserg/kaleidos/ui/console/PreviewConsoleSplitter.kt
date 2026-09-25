package com.makeevrserg.kaleidos.ui.console

import com.intellij.ui.OnePixelSplitter
import javax.swing.JComponent

class PreviewConsoleSplitter(
    page: JComponent,
    private val console: JComponent
) {
    private val splitter = OnePixelSplitter(true, PROPORTION_KEY, DEFAULT_PAGE_PROPORTION)

    val component: JComponent = splitter

    var isConsoleVisible: Boolean
        get() = splitter.secondComponent != null
        set(isVisible) {
            splitter.secondComponent = if (isVisible) console else null
        }

    init {
        splitter.firstComponent = page
    }

    private companion object {
        const val PROPORTION_KEY = "Kaleidos.DevServerConsole.Proportion"
        const val DEFAULT_PAGE_PROPORTION = 0.7f
    }
}
