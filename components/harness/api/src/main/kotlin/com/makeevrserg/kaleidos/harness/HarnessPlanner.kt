package com.makeevrserg.kaleidos.harness

import com.makeevrserg.kaleidos.host.DevServerKind
import com.makeevrserg.kaleidos.host.PreviewHost
import com.makeevrserg.kaleidos.host.PreviewProjectStructure

/**
 * Decides what to generate where: a registry in every module the host can see previews in, and the page
 * in the host itself. Modules the host does not depend on are left out, their previews would not compile.
 */
class HarnessPlanner(
    private val projectPreviewSource: ProjectPreviewSource,
    private val portAllocator: FreePortAllocator
) {

    private fun ModulePreviews.toModulePlan(gradlePath: String): PreviewModulePlan {
        return PreviewModulePlan(
            gradlePath = gradlePath,
            directory = moduleDirectory,
            previews = previews.sortedBy { preview -> preview.fqn }
        )
    }

    private fun previewModules(
        host: PreviewHost,
        structure: PreviewProjectStructure,
        previews: List<ModulePreviews>
    ): List<PreviewModulePlan> {
        return previews
            .filter { modulePreviews -> modulePreviews.previews.isNotEmpty() }
            .filter { modulePreviews -> structure.reaches(host.directory, modulePreviews.moduleDirectory) }
            .mapNotNull { modulePreviews ->
                structure.gradlePathOf(modulePreviews.moduleDirectory)
                    ?.let { gradlePath -> modulePreviews.toModulePlan(gradlePath) }
            }
            .sortedBy { module -> module.gradlePath }
    }

    private suspend fun hostPlan(host: PreviewHost): HarnessHostPlan {
        val isWebpack = host.kind == DevServerKind.WEBPACK
        return HarnessHostPlan(
            gradlePath = host.gradlePath,
            directory = host.directory,
            rootProjectPath = host.rootProjectPath,
            kind = host.kind,
            devServerPort = if (isWebpack) portAllocator.allocate(host.directory) else null,
            excludedSourcePaths = if (isWebpack) {
                projectPreviewSource.entryPointSourcePaths(host.directory)
            } else {
                emptyList()
            }
        )
    }

    suspend fun plan(host: PreviewHost, structure: PreviewProjectStructure): HarnessPlan {
        return HarnessPlan(
            host = hostPlan(host),
            modules = previewModules(host, structure, projectPreviewSource.previews())
        )
    }
}
