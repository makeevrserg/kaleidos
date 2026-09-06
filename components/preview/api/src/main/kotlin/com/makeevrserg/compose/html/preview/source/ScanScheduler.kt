package com.makeevrserg.compose.html.preview.source

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlin.time.Duration

/**
 * Decides which file to scan for previews and when: the selected file at once, and again after its
 * edits settle for [rescanDebounce]. Edits of other files are ignored, and a new selection drops the
 * pending rescan of the previous one.
 *
 * @param File whatever the host uses to identify an editor file; compared by equality
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class ScanScheduler(
    private val rescanDebounce: Duration
) {
    /**
     * @param selectedFiles null when no editor is open
     * @return null when nothing is selected, so the consumer can clear its state
     */
    fun <File : Any> filesToScan(selectedFiles: Flow<File?>, editedFiles: Flow<File>): Flow<File?> {
        return selectedFiles.flatMapLatest { selected ->
            if (selected == null) return@flatMapLatest flowOf(null)
            editedFiles
                .filter { edited -> edited == selected }
                .debounce(rescanDebounce)
                .map { selected }
                .onStart { emit(selected) }
        }
    }
}
