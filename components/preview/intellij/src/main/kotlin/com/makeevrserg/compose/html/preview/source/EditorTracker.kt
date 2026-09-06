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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Makes the preview follow the editor: the selected file is scanned for `@Preview` functions on
 * every selection change and, debounced, on every edit of that file, so new previews appear without
 * any click.
 */
class EditorTracker(
    private val projectDependencies: ProjectDependencies,
    private val fileScanner: PreviewFileScanner,
    private val contract: PreviewStore,
    coroutineFeature: CoroutineFeature
) : FileEditorManagerListener, DocumentListener, CoroutineFeature by coroutineFeature {

    /** Only touched on the event dispatch thread, where both listeners are invoked. */
    private var currentFile: VirtualFile? = null

    private var scanJob: Job? = null

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

    private fun scheduleScan(file: VirtualFile, debounce: Boolean) {
        scanJob?.cancel()
        scanJob = launch {
            if (debounce) delay(RESCAN_DEBOUNCE)
            contract.onFileSelected(scan(file))
        }
    }

    private fun select(file: VirtualFile?) {
        currentFile = file
        if (file == null) {
            scanJob?.cancel()
            contract.onFileSelected(null)
            return
        }
        scheduleScan(file, debounce = false)
    }

    override fun selectionChanged(event: FileEditorManagerEvent) = select(event.newFile)

    override fun documentChanged(event: DocumentEvent) {
        val file = projectDependencies.fileDocumentManager.getFile(event.document) ?: return
        if (file == currentFile) scheduleScan(file, debounce = true)
    }

    fun start(parentDisposable: Disposable) {
        projectDependencies.messageBus.connect(parentDisposable)
            .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this)
        projectDependencies.editorEventMulticaster.addDocumentListener(this, parentDisposable)
        select(projectDependencies.fileEditorManager.selectedFiles.firstOrNull())
    }

    private companion object {
        val RESCAN_DEBOUNCE = 300.milliseconds
    }
}
