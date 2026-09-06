package com.makeevrserg.compose.html.preview.source

import com.intellij.openapi.vfs.VirtualFile

/** An editor event as reported by the IDE; [EditorTracker] processes them strictly in arrival order. */
sealed interface EditorEvent {
    /** [file] is null when the last editor tab was closed. */
    data class Selected(val file: VirtualFile?) : EditorEvent

    data class Edited(val file: VirtualFile) : EditorEvent
}
