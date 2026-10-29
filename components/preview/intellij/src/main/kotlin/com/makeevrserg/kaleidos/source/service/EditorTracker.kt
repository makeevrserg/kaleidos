package com.makeevrserg.kaleidos.source.service

import com.intellij.openapi.application.readAction
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.makeevrserg.kaleidos.dependencies.ProjectDependencies
import com.makeevrserg.kaleidos.feature.api.PreviewStore
import com.makeevrserg.kaleidos.feature.model.PreviewScan
import com.makeevrserg.kaleidos.feature.model.PreviewTarget
import com.makeevrserg.kaleidos.source.api.ScanScheduler
import com.makeevrserg.kaleidos.source.api.SelectedFileScanner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * Makes the preview follow editor selections and debounced edits. A new selection reports
 * [PreviewScan.Pending] before its scan, while a rescan keeps the current page visible. Cold flows
 * own the IDE listeners, and `transformLatest` cancels obsolete scans.
 *
 * @param mainContext [com.makeevrserg.kaleidos.core.coroutine.api.PreviewDispatchers.main]
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class EditorTracker(
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

    private suspend fun scan(file: VirtualFile): PreviewTarget {
        return readAction {
            targetOf(file, fileScanner.scan(file.path))
        }
    }

    /**
     * The selected file, current one first. Subscribing and reading the selection happen together on
     * the UI thread, so no selection change can slip in between and be overwritten by a stale snapshot.
     */
    private fun selectedFiles(): Flow<VirtualFile?> {
        return callbackFlow {
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
    }

    private fun editedFiles(): Flow<VirtualFile> {
        return callbackFlow {
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
