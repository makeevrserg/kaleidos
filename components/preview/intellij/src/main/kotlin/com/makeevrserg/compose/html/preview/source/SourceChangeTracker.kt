package com.makeevrserg.compose.html.preview.source

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.vfs.VirtualFile
import com.makeevrserg.compose.html.preview.dependencies.ProjectDependencies
import com.makeevrserg.compose.html.preview.feature.PreviewStore

/**
 * Reports edits of Kotlin sources of the project to the store. Any Kotlin file counts: a preview
 * depends on components and helpers from other files, and resolving the exact dependency graph is not
 * worth the cost for a freshness hint.
 *
 * Whole-text replacements are skipped: that is how the IDE reloads a file changed on disk, and such a
 * change is usually already built and live-reloaded by the time the IDE notices it. Reporting it would
 * mark a fresh page as stale.
 */
class SourceChangeTracker(
    private val projectDependencies: ProjectDependencies,
    private val contract: PreviewStore
) : DocumentListener {

    private fun VirtualFile.isProjectKotlinSource(): Boolean {
        return extension in KOTLIN_EXTENSIONS && projectDependencies.fileIndex.isInContent(this)
    }

    override fun documentChanged(event: DocumentEvent) {
        if (event.isWholeTextReplaced) return
        val file = projectDependencies.fileDocumentManager.getFile(event.document) ?: return
        if (!file.isProjectKotlinSource()) return
        contract.onSourceChanged(file.name)
    }

    /** The listener lives as long as [parentDisposable]; the project service is the natural owner. */
    fun start(parentDisposable: Disposable) {
        projectDependencies.editorEventMulticaster.addDocumentListener(this, parentDisposable)
    }

    private companion object {
        val KOTLIN_EXTENSIONS = setOf("kt", "kts")
    }
}
