package com.makeevrserg.compose.html.preview.source

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.readAction
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile
import com.makeevrserg.compose.html.preview.core.CoroutineFeature
import com.makeevrserg.compose.html.preview.dependencies.ProjectDependencies
import com.makeevrserg.compose.html.preview.feature.PreviewStore
import com.makeevrserg.compose.html.preview.feature.PreviewTarget
import com.makeevrserg.compose.html.preview.psi.PreviewFileScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds

/**
 * Makes the preview follow the editor: the selected file is scanned for `@Preview` functions on
 * every selection change and, debounced, on every edit of that file, so new previews appear without
 * any click.
 *
 * Thread safety: the IDE calls the listeners on the event dispatch thread while [start] runs on the
 * lifecycle thread, so the listeners only enqueue [EditorEvent]s and a single coroutine consumes them
 * in order and owns all mutable state. Subscribing and reading the initial selection happen together
 * on the UI thread, so no selection change can slip in between and be overwritten by a stale snapshot.
 *
 * @param mainContext [com.makeevrserg.compose.html.preview.core.PreviewDispatchers.main]
 */
class EditorTracker(
    private val projectDependencies: ProjectDependencies,
    private val fileScanner: PreviewFileScanner,
    private val contract: PreviewStore,
    private val mainContext: CoroutineContext,
    coroutineFeature: CoroutineFeature
) : FileEditorManagerListener, DocumentListener, CoroutineFeature by coroutineFeature {

    private val events = Channel<EditorEvent>(Channel.UNLIMITED)

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
     * Cancels the previous scan and waits for it to finish before starting the next one, so results
     * always reach the store in the order the scans were requested.
     */
    private suspend fun CoroutineScope.restartScan(previous: Job?, file: VirtualFile, debounce: Duration): Job {
        previous?.cancelAndJoin()
        return launch {
            delay(debounce)
            contract.onFileSelected(scan(file))
        }
    }

    private suspend fun trackEvents() = coroutineScope {
        var currentFile: VirtualFile? = null
        var scanJob: Job? = null
        for (event in events) {
            when (event) {
                is EditorEvent.Selected -> {
                    currentFile = event.file
                    val file = event.file
                    if (file == null) {
                        scanJob?.cancelAndJoin()
                        contract.onFileSelected(null)
                    } else {
                        scanJob = restartScan(scanJob, file, debounce = ZERO)
                    }
                }

                is EditorEvent.Edited -> {
                    if (event.file != currentFile) continue
                    scanJob = restartScan(scanJob, event.file, debounce = RESCAN_DEBOUNCE)
                }
            }
        }
    }

    private suspend fun subscribe(parentDisposable: Disposable) = withContext(mainContext) {
        projectDependencies.messageBus.connect(parentDisposable)
            .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this@EditorTracker)
        projectDependencies.editorEventMulticaster.addDocumentListener(this@EditorTracker, parentDisposable)
        events.send(EditorEvent.Selected(projectDependencies.fileEditorManager.selectedFiles.firstOrNull()))
    }

    override fun selectionChanged(event: FileEditorManagerEvent) {
        events.trySend(EditorEvent.Selected(event.newFile))
    }

    override fun documentChanged(event: DocumentEvent) {
        val file = projectDependencies.fileDocumentManager.getFile(event.document) ?: return
        events.trySend(EditorEvent.Edited(file))
    }

    fun start(parentDisposable: Disposable) {
        launch {
            subscribe(parentDisposable)
            trackEvents()
        }
    }

    private companion object {
        val RESCAN_DEBOUNCE = 300.milliseconds
    }
}
