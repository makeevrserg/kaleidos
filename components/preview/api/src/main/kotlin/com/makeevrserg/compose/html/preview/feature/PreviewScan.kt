package com.makeevrserg.compose.html.preview.feature

/**
 * What the plugin knows about the previews of the selected editor file.
 *
 * The selection is reported before the file is read, so [Pending] is what the tool window shows while
 * the scan runs: the previews of the previous file are gone from the state at that moment already.
 */
sealed interface PreviewScan {

    /** The file has just been selected and has not been read yet. */
    data object Pending : PreviewScan

    /** The file declares no `@Preview` function. */
    data object NoPreviews : PreviewScan

    /**
     * The file declares previews the generated page cannot call: its source set is not compiled into
     * the Kotlin/JS output of the module, `jvmMain` and every test source set for example.
     *
     * @param sourceSetName source set the file belongs to, as the Gradle sync named it
     */
    data class UnsupportedSourceSet(
        val sourceSetName: String
    ) : PreviewScan

    /** @param previews previews of the file in source order; never empty */
    data class Renderable(
        val previews: List<PreviewFunction>
    ) : PreviewScan
}
