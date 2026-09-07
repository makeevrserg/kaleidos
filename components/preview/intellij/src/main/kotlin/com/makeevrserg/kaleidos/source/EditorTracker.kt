package com.makeevrserg.kaleidos.source

import com.intellij.openapi.application.readAction
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.makeevrserg.kaleidos.dependencies.ProjectDependencies
import com.makeevrserg.kaleidos.feature.PreviewScan
import com.makeevrserg.kaleidos.feature.PreviewStore
import com.makeevrserg.kaleidos.feature.PreviewTarget
import com.makeevrserg.kaleidos.psi.SelectedFileScanner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * Makes the preview follow the editor: the selected file is scanned for `@Preview` functions on
 * every selection change and, debounced, on every edit of that file, so new previews appear without
 * any click.
 *
 * A newly selected file is reported twice: [PreviewScan.Pending] before the scan, so the page of the
 * previous file leaves the tool window the moment the editor switches, and the scan result after it.
 * A rescan of the file that is already shown skips the first report and never blanks its page.
 *
 * Editor events are cold flows: the IDE listeners exist only while [track] runs and are removed when
 * it is cancelled, so the listeners and their consumer cannot outlive each other. `transformLatest`
 * cancels a scan that a newer selection made obsolete, so results reach the store in request order.
 *
 * @param mainContext [com.makeevrserg.kaleidos.core.PreviewDispatchers.main]
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EditorTracker(
    private val projectDependencies: ProjectDependencies,
    private val fileScanner: SelectedFileScanner,
    private val scanScheduler: ScanScheduler,
    private val contract: PreviewStore,
    private val mainContext: CoroutineContext
) {

    private fun targetOf(file: VirtualFile, scan: PreviewScan): PreviewTarget {
        return PreviewTarget(
            filePath = file.path,
            fileName = file.name,
            scan = scan,
            focusedFqn = null,
            host = null
        )
    }

    private suspend fun scan(file: VirtualFile): PreviewTarget = readAction {
        targetOf(file, fileScanner.scan(file))
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
        val subscription = Disposer.newDisposable("Kaleidos editor selection")
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
        val subscription = Disposer.newDisposable("Kaleidos document edits")
        projectDependencies.editorEventMulticaster.addDocumentListener(listener, subscription)
        awaitClose { Disposer.dispose(subscription) }
    }

    /** Follows the editor until cancelled. Must run off the event dispatch thread: scans take a read action. */
    suspend fun track() {
        scanScheduler.scanRequests(selectedFiles(), editedFiles())
            .transformLatest { request ->
                val file = request.file
                if (file == null) {
                    emit(null)
                    return@transformLatest
                }
                if (request.isNewSelection) emit(targetOf(file, PreviewScan.Pending))
                emit(scan(file))
            }
            .collect(contract::onFileSelected)
    }
}
