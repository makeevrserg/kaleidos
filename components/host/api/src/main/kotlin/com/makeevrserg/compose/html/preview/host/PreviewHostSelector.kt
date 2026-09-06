package com.makeevrserg.compose.html.preview.host

/**
 * Picks the module that serves the previews of a file, the way the official Compose preview finds
 * the Gradle module of a file, extended to library modules: any module with a dev server task that
 * contains the file or depends on its module qualifies. Real applications of the project usually
 * qualify too, so a module whose path mentions "preview" wins when several remain.
 */
class PreviewHostSelector {

    private fun GradleModule.toHostCandidate(): HostCandidate? {
        return DevServerKind.detect(taskNames)?.let { kind -> HostCandidate(module = this, kind = kind) }
    }

    private fun HostCandidate.toPreviewHost(): PreviewHost {
        return PreviewHost(
            gradlePath = module.gradlePath,
            directory = module.directory,
            rootProjectPath = module.rootProjectPath,
            kind = kind
        )
    }

    private fun List<HostCandidate>.toDisplayNames(): String {
        return joinToString { candidate -> candidate.toPreviewHost().displayName }
    }

    private fun List<HostCandidate>.serving(
        fileModuleDirectory: String,
        graph: ModuleDependencyGraph
    ): List<HostCandidate> {
        return filter { candidate ->
            candidate.module.directory == fileModuleDirectory ||
                graph.dependsOn(candidate.module.directory, fileModuleDirectory)
        }
    }

    private fun List<HostCandidate>.preferPreviewModules(): List<HostCandidate> {
        val previewModules = filter { candidate ->
            candidate.module.gradlePath.contains(PREVIEW_MARKER, ignoreCase = true)
        }
        return previewModules.ifEmpty { this }
    }

    private fun List<HostCandidate>.preferOwnModule(fileModuleDirectory: String): List<HostCandidate> {
        val ownModule = filter { candidate -> candidate.module.directory == fileModuleDirectory }
        return ownModule.ifEmpty { this }
    }

    private fun noCandidateReason(servers: List<HostCandidate>): String {
        if (servers.isEmpty()) {
            return "No Kotlin/JS browser or Kobweb module found in the project. " +
                "Add a preview module with a dev server, see the plugin README."
        }
        return "No module with a dev server depends on the module of this file. " +
            "The preview module has to depend on it. Modules with a dev server: ${servers.toDisplayNames()}"
    }

    private fun ambiguousReason(candidates: List<HostCandidate>): String {
        return "Several modules could serve the preview: ${candidates.toDisplayNames()}. " +
            "Give the preview module a path containing \"preview\"."
    }

    fun select(
        fileModuleDirectory: String,
        modules: List<GradleModule>,
        graph: ModuleDependencyGraph
    ): PreviewHostResolution {
        if (modules.isEmpty()) return PreviewHostResolution.NotFound(GRADLE_NOT_IMPORTED)
        val servers = modules.mapNotNull { module -> module.toHostCandidate() }
        val candidates = servers.serving(fileModuleDirectory, graph)
            .preferPreviewModules()
            .preferOwnModule(fileModuleDirectory)
        return when (candidates.size) {
            0 -> PreviewHostResolution.NotFound(noCandidateReason(servers))
            1 -> PreviewHostResolution.Found(candidates.single().toPreviewHost())
            else -> PreviewHostResolution.NotFound(ambiguousReason(candidates))
        }
    }

    companion object {
        private const val PREVIEW_MARKER = "preview"
        const val GRADLE_NOT_IMPORTED =
            "Gradle project is not imported yet. Wait for the sync to finish, then press Refresh Preview."
    }
}
