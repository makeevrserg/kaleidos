package com.makeevrserg.kaleidos.harness.usecase

import com.makeevrserg.kaleidos.harness.api.FreePortAllocator
import com.makeevrserg.kaleidos.harness.api.ProjectPreviewSource
import com.makeevrserg.kaleidos.harness.model.HarnessHostPlan
import com.makeevrserg.kaleidos.harness.model.HarnessPlan
import com.makeevrserg.kaleidos.harness.model.HarnessPreview
import com.makeevrserg.kaleidos.harness.model.ModulePreviews
import com.makeevrserg.kaleidos.harness.model.PreviewModulePlan
import com.makeevrserg.kaleidos.harness.model.PrivatePreviewFile
import com.makeevrserg.kaleidos.host.model.DevServerKind
import com.makeevrserg.kaleidos.host.model.PreviewHost
import com.makeevrserg.kaleidos.host.model.PreviewProjectStructure
import com.makeevrserg.kaleidos.host.util.gradlePathOf
import com.makeevrserg.kaleidos.host.util.reaches

/**
 * Decides what to generate where: a registry in every module the host can see previews in, and the page
 * in the host itself. Modules the host does not depend on are left out, their previews would not compile.
 */
internal class PlanHarnessUseCase(
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
                .sortedBy(HarnessPreview::fqn),
            privatePreviewFiles = privatePreviewFiles
                .filterNot { file -> file.sourcePath in excludedSourcePaths }
                .sortedBy(PrivatePreviewFile::sourcePath)
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
            .sortedBy(PreviewModulePlan::gradlePath)
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

    suspend operator fun invoke(host: PreviewHost, structure: PreviewProjectStructure): HarnessPlan {
        val hostPlan = hostPlan(host)
        return HarnessPlan(
            host = hostPlan,
            modules = previewModules(
                hostPlan,
                structure,
                projectPreviewSource.previews()
            )
        )
    }
}
