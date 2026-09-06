package com.makeevrserg.compose.html.preview.ui

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object PreviewIcons {
    @JvmField
    val Gutter: Icon = IconLoader.getIcon("/icons/preview.svg", PreviewIcons::class.java)

    @JvmField
    val ToolWindow: Icon = IconLoader.getIcon("/icons/toolWindowPreview.svg", PreviewIcons::class.java)
}
