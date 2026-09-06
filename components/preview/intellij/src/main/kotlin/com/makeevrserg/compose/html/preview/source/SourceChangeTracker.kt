package com.makeevrserg.compose.html.preview.source

import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.makeevrserg.compose.html.preview.dependencies.ProjectDependencies
import com.makeevrserg.compose.html.preview.feature.PreviewStore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

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
) {
    private fun VirtualFile.isProjectKotlinSource(): Boolean {
        return extension in KOTLIN_EXTENSIONS && projectDependencies.fileIndex.isInContent(this)
    }

    /** Names of edited project Kotlin files; the listener exists only while collected. */
    private fun changedFileNames(): Flow<String> = callbackFlow {
        val listener = object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                if (event.isWholeTextReplaced) return
                val file = projectDependencies.fileDocumentManager.getFile(event.document) ?: return
                if (file.isProjectKotlinSource()) trySend(file.name)
            }
        }
        val subscription = Disposer.newDisposable("Compose HTML Preview source changes")
        projectDependencies.editorEventMulticaster.addDocumentListener(listener, subscription)
        awaitClose { Disposer.dispose(subscription) }
    }

    /** Reports edits until cancelled. */
    suspend fun track() = changedFileNames().collect(contract::onSourceChanged)

    private companion object {
        val KOTLIN_EXTENSIONS = setOf("kt", "kts")
    }
}
