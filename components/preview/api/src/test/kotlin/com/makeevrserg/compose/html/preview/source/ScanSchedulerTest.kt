package com.makeevrserg.compose.html.preview.source

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class ScanSchedulerTest {
    private val scheduler = ScanScheduler(rescanDebounce = DEBOUNCE)

    private val selectedFiles = MutableStateFlow<String?>(null)

    private val editedFiles = MutableSharedFlow<String>()

    private val scanned = mutableListOf<String?>()

    private fun TestScope.startCollecting() {
        backgroundScope.launch { scheduler.filesToScan(selectedFiles, editedFiles).toList(scanned) }
        runCurrent()
    }

    private suspend fun TestScope.edit(file: String) {
        editedFiles.emit(file)
        runCurrent()
    }

    @Test
    fun GIVEN_file_selected_WHEN_collected_THEN_scanned_at_once() = runTest {
        selectedFiles.value = "A.kt"

        startCollecting()

        assertEquals(listOf<String?>("A.kt"), scanned)
    }

    @Test
    fun GIVEN_no_editor_WHEN_collected_THEN_null_is_reported_so_the_consumer_clears() = runTest {
        startCollecting()

        assertEquals(listOf<String?>(null), scanned)
    }

    @Test
    fun GIVEN_selected_file_edited_WHEN_debounce_passes_THEN_scanned_again() = runTest {
        selectedFiles.value = "A.kt"
        startCollecting()

        edit("A.kt")
        assertEquals(listOf<String?>("A.kt"), scanned)
        advanceTimeBy(DEBOUNCE + 1.milliseconds)
        runCurrent()

        assertEquals(listOf<String?>("A.kt", "A.kt"), scanned)
    }

    @Test
    fun GIVEN_rapid_edits_WHEN_debounce_passes_THEN_one_rescan() = runTest {
        selectedFiles.value = "A.kt"
        startCollecting()

        edit("A.kt")
        advanceTimeBy(DEBOUNCE / 2)
        edit("A.kt")
        advanceTimeBy(DEBOUNCE / 2)
        edit("A.kt")
        advanceTimeBy(DEBOUNCE + 1.milliseconds)
        runCurrent()

        assertEquals(listOf<String?>("A.kt", "A.kt"), scanned)
    }

    @Test
    fun GIVEN_another_file_edited_WHEN_debounce_passes_THEN_no_rescan() = runTest {
        selectedFiles.value = "A.kt"
        startCollecting()

        edit("B.kt")
        advanceTimeBy(DEBOUNCE + 1.milliseconds)
        runCurrent()

        assertEquals(listOf<String?>("A.kt"), scanned)
    }

    @Test
    fun GIVEN_pending_rescan_WHEN_another_file_selected_THEN_old_rescan_dropped_and_new_file_scanned() = runTest {
        selectedFiles.value = "A.kt"
        startCollecting()
        edit("A.kt")

        selectedFiles.value = "B.kt"
        runCurrent()
        advanceTimeBy(DEBOUNCE + 1.milliseconds)
        runCurrent()

        assertEquals(listOf<String?>("A.kt", "B.kt"), scanned)
    }

    @Test
    fun GIVEN_last_editor_closed_WHEN_previous_file_edited_THEN_null_reported_and_no_rescan() = runTest {
        selectedFiles.value = "A.kt"
        startCollecting()

        selectedFiles.value = null
        runCurrent()
        edit("A.kt")
        advanceTimeBy(DEBOUNCE + 1.milliseconds)
        runCurrent()

        assertEquals(listOf<String?>("A.kt", null), scanned)
    }

    private companion object {
        val DEBOUNCE = 300.milliseconds
    }
}
