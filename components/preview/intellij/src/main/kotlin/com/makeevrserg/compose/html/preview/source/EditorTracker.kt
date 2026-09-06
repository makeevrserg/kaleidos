package com.makeevrserg.compose.html.preview.source

import com.intellij.openapi.application.readAction
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.makeevrserg.compose.html.preview.dependencies.ProjectDependencies
import com.makeevrserg.compose.html.preview.feature.PreviewStore
import com.makeevrserg.compose.html.preview.feature.PreviewTarget
import com.makeevrserg.compose.html.preview.psi.PreviewFileScanner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * Makes the preview follow the editor: the selected file is scanned for `@Preview` functions on
 * every selection change and, debounced, on every edit of that file, so new previews appear without
 * any click.
 *
 * Editor events are cold flows: the IDE listeners exist only while [track] runs and are removed when
 * it is cancelled, so the listeners and their consumer cannot outlive each other. `mapLatest` cancels
 * a scan that a newer selection made obsolete, so results reach the store in request order.
 *
 * @param mainContext [com.makeevrserg.compose.html.preview.core.PreviewDispatchers.main]
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EditorTracker(
    private val projectDependencies: ProjectDependencies,
    private val fileScanner: PreviewFileScanner,
    private val scanScheduler: ScanScheduler,
    private val contract: PreviewStore,
    private val mainContext: CoroutineContext
) {
    private suspend fun scan(file: VirtualFile): PreviewTarget = readAction {
        val previews = projectDependencies.psiManager.findFile(file)?.let(fileScanner::scan).orEmpty()
        PreviewTarget(
            filePath = file.path,
            fileName = file.name,
            previews = previews,
            focusedFqn = null,
            host = null
        )
    }

    /**
     * The selected file, current one first. Subscribing and reading the selection happen together on
     * the UI thread, so no selection change can slip in between and be overwritten by a stale snapshot.
     */
    private fun selectedFiles(): Flow<VirtualFile?> = callbackFlow {
        val listener = object : FileEditorManagerListener {
            override fun selectionChanged(event: FileEditorManagerEvent) {
                trySend(event.newFile)
            }
        }
        val subscription = Disposer.newDisposable("Compose HTML Preview editor selection")
        withContext(mainContext) {
            projectDependencies.messageBus.connect(subscription)
                .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, listener)
            trySend(projectDependencies.fileEditorManager.selectedFiles.firstOrNull())
        }
        awaitClose { Disposer.dispose(subscription) }
    }

    private fun editedFiles(): Flow<VirtualFile> = callbackFlow {
        val listener = object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val file = projectDependencies.fileDocumentManager.getFile(event.document) ?: return
                trySend(file)
            }
        }
        val subscription = Disposer.newDisposable("Compose HTML Preview document edits")
        projectDependencies.editorEventMulticaster.addDocumentListener(listener, subscription)
        awaitClose { Disposer.dispose(subscription) }
    }

    /** Follows the editor until cancelled. Must run off the event dispatch thread: scans take a read action. */
    suspend fun track() {
        scanScheduler.filesToScan(selectedFiles(), editedFiles())
            .mapLatest { file -> file?.let { selected -> scan(selected) } }
            .collect(contract::onFileSelected)
    }
}
