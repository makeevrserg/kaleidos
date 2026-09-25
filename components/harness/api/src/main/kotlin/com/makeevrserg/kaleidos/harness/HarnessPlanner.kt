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

    private fun HarnessHostPlan.excludedSourcePathsIn(moduleDirectory: String): List<String> {
        return if (moduleDirectory == directory) excludedSourcePaths else emptyList()
    }

    private fun ModulePreviews.toModulePlan(gradlePath: String, excludedSourcePaths: List<String>): PreviewModulePlan {
        return PreviewModulePlan(
            gradlePath = gradlePath,
            directory = moduleDirectory,
            previews = previews
                .filterNot { preview -> preview.sourcePath in excludedSourcePaths }
                .sortedBy { preview -> preview.fqn },
            privatePreviewFiles = privatePreviewFiles
                .filterNot { file -> file.sourcePath in excludedSourcePaths }
                .sortedBy { file -> file.sourcePath }
        )
    }

    private fun previewModules(
        host: HarnessHostPlan,
        structure: PreviewProjectStructure,
        previews: List<ModulePreviews>
    ): List<PreviewModulePlan> {
        return previews
            .filter { modulePreviews -> structure.reaches(host.directory, modulePreviews.moduleDirectory) }
            .mapNotNull { modulePreviews ->
                structure.gradlePathOf(modulePreviews.moduleDirectory)?.let { gradlePath ->
                    modulePreviews.toModulePlan(gradlePath, host.excludedSourcePathsIn(modulePreviews.moduleDirectory))
                }
            }
            .filter { module -> module.previews.isNotEmpty() }
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
        val hostPlan = hostPlan(host)
        return HarnessPlan(
            host = hostPlan,
            modules = previewModules(hostPlan, structure, projectPreviewSource.previews())
        )
    }
}
