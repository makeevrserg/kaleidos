package com.makeevrserg.compose.html.preview.harness

import com.makeevrserg.compose.html.preview.host.PreviewHost

/**
 * Generates the preview application into the build directories of the project. Called before every
 * launch, so a preview added or removed in the editor is on the page after the next recompilation.
 */
interface PreviewHarness {
    suspend fun install(host: PreviewHost): Result<HarnessInstallation>
}
