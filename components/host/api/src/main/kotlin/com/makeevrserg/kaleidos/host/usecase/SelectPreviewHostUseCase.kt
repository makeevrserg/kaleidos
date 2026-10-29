package com.makeevrserg.kaleidos.host.usecase

import com.makeevrserg.kaleidos.host.model.DevServerKind
import com.makeevrserg.kaleidos.host.model.GradleModule
import com.makeevrserg.kaleidos.host.model.GradleModuleKind
import com.makeevrserg.kaleidos.host.model.HostCandidate
import com.makeevrserg.kaleidos.host.model.PreviewHost
import com.makeevrserg.kaleidos.host.model.PreviewHostResolution
import com.makeevrserg.kaleidos.host.model.PreviewProjectStructure
import com.makeevrserg.kaleidos.host.util.reaches

/**
 * Picks the module whose dev server shows the previews of a file. Nothing is configured and nothing has
 * to be written by hand: the plugin generates the preview page into the module it picks here.
 *
 * A Kotlin/JS browser module previews its own files, which is also what makes previews of `internal`
 * functions work. A Kobweb library cannot: the code that registers the styles of Silk components is
 * generated for applications only, so an application of the build that depends on the module serves it.
 */
class SelectPreviewHostUseCase {

    private fun toCandidate(module: GradleModule): HostCandidate? {
        val kind = GradleModuleKind.detect(module.taskNames).devServerKind ?: return null
        return HostCandidate(module = module, kind = kind)
    }

    private fun HostCandidate.toPreviewHost(): PreviewHost {
        return PreviewHost(
            gradlePath = module.gradlePath,
            directory = module.directory,
            rootProjectPath = module.rootProjectPath,
            kind = kind
        )
    }

    private fun List<HostCandidate>.preferOwnModule(fileModuleDirectory: String): List<HostCandidate> {
        val ownModule = filter { candidate -> candidate.module.directory == fileModuleDirectory }
        return ownModule.ifEmpty { this }
    }

    private fun List<HostCandidate>.preferPreviewModules(): List<HostCandidate> {
        val previewModules = filter { candidate ->
            candidate.module.gradlePath.contains(PREVIEW_MARKER, ignoreCase = true)
        }
        return previewModules.ifEmpty { this }
    }

    /** Shortest path first, then alphabetically, so the same module is picked on every lookup. */
    private fun List<HostCandidate>.firstStable(): HostCandidate? {
        return minWithOrNull(
            compareBy({ candidate -> candidate.module.gradlePath.length }, { candidate -> candidate.module.gradlePath })
        )
    }

    /**
     * Applications that can show the file: the module itself when it can serve a page, and every module
     * that depends on it. A Kobweb library is served by Kobweb applications only.
     */
    private fun servingCandidates(
        fileModule: GradleModule,
        structure: PreviewProjectStructure
    ): List<HostCandidate> {
        val isKobwebLibrary = GradleModuleKind.detect(fileModule.taskNames) == GradleModuleKind.KOBWEB_LIBRARY
        return structure.modules
            .mapNotNull(::toCandidate)
            .filter { candidate -> !isKobwebLibrary || candidate.kind == DevServerKind.KOBWEB }
            .filter { candidate -> structure.reaches(candidate.module.directory, fileModule.directory) }
    }

    private fun noCandidateReason(fileModule: GradleModule): String {
        val isKobwebLibrary = GradleModuleKind.detect(fileModule.taskNames) == GradleModuleKind.KOBWEB_LIBRARY
        if (isKobwebLibrary) {
            return "No Kobweb application of this project depends on ${fileModule.displayName}. " +
                "Kobweb components are styled by the generated entry point of an application, so one of " +
                "your Kobweb applications has to depend on this module."
        }
        return "${fileModule.displayName} has no Kotlin/JS browser target and no module that has one " +
            "depends on it, so there is nothing that could render the preview."
    }

    operator fun invoke(fileModuleDirectory: String, structure: PreviewProjectStructure): PreviewHostResolution {
        if (!structure.isImported) return PreviewHostResolution.NotFound(GRADLE_NOT_IMPORTED)
        val fileModule = structure.modules.firstOrNull { module -> module.directory == fileModuleDirectory }
            ?: return PreviewHostResolution.NotFound(GRADLE_NOT_IMPORTED)
        val candidate = servingCandidates(fileModule, structure)
            .preferOwnModule(fileModuleDirectory)
            .preferPreviewModules()
            .firstStable()
            ?: return PreviewHostResolution.NotFound(noCandidateReason(fileModule))
        return PreviewHostResolution.Found(candidate.toPreviewHost())
    }

    companion object {
        private const val PREVIEW_MARKER = "preview"
        const val GRADLE_NOT_IMPORTED =
            "Gradle project is not imported yet. Wait for the sync to finish, then press Refresh Preview."
    }
}
