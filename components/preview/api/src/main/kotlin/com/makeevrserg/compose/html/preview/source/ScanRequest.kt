package com.makeevrserg.compose.html.preview.source

/**
 * One file the consumer should scan for previews.
 *
 * @param file null when no editor file is selected
 * @param isNewSelection true when [file] has just become the selected file, so whatever is shown for
 * the previous one must go; false for a rescan of the file that is already shown, which must not
 * interrupt it
 */
data class ScanRequest<File : Any>(
    val file: File?,
    val isNewSelection: Boolean
)
