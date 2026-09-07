package com.makeevrserg.kaleidos.harness

/** Everything the generated preview page needs to know about the sources of the project. */
interface ProjectPreviewSource {
    suspend fun previews(): List<ModulePreviews>

    /**
     * Files of a module that declare a top-level `main`, relative to their source root
     * (`com/example/Main.kt`). The preview page brings its own entry point, so these are left out of
     * the compilation while the preview runs.
     */
    suspend fun entryPointSourcePaths(moduleDirectory: String): List<String>
}
