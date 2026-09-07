package com.makeevrserg.compose.html.preview.source

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
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
     * @param selectedFiles null when no editor is open; repeated reports of the same file, which split
     * editors produce, are not selection changes and are dropped
     */
    fun <File : Any> scanRequests(
        selectedFiles: Flow<File?>,
        editedFiles: Flow<File>
    ): Flow<ScanRequest<File>> {
        return selectedFiles
            .distinctUntilChanged()
            .flatMapLatest { selected ->
                if (selected == null) {
                    return@flatMapLatest flowOf(ScanRequest(file = null, isNewSelection = true))
                }
                editedFiles
                    .filter { edited -> edited == selected }
                    .debounce(rescanDebounce)
                    .map { ScanRequest(file = selected, isNewSelection = false) }
                    .onStart { emit(ScanRequest(file = selected, isNewSelection = true)) }
            }
    }
}
