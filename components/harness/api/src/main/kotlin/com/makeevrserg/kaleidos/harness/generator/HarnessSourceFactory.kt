package com.makeevrserg.kaleidos.harness.generator

import com.makeevrserg.kaleidos.harness.HarnessPlan
import com.makeevrserg.kaleidos.harness.PreviewModulePlan

/**
 * Turns a plan into the files that have to be on disk: a registry per module, the page and the init
 * script in the host. The host may own previews itself, in which case its registry joins its page.
 */
class HarnessSourceFactory(
    private val registryFactory: PreviewRegistryFactory,
    private val pageFactory: PreviewPageFactory,
    private val initScriptFactory: PreviewInitScriptFactory,
    private val privatePreviewFileFactory: PrivatePreviewFileFactory
) {

    private fun moduleFiles(module: PreviewModulePlan): List<GeneratedFile> {
        return listOf(registryFactory.create(module)) + privatePreviewFileFactory.create(module)
    }

    private fun hostFiles(plan: HarnessPlan): List<GeneratedFile> {
        val moduleFiles = plan.modules
            .filter { module -> module.directory == plan.host.directory }
            .flatMap(::moduleFiles)
        return moduleFiles + pageFactory.create(plan) + initScriptFactory.create(plan)
    }

    fun create(plan: HarnessPlan): List<GeneratedModuleSources> {
        val registryOnlyModules = plan.modules
            .filterNot { module -> module.directory == plan.host.directory }
            .map { module ->
                GeneratedModuleSources(
                    moduleDirectory = module.directory,
                    files = moduleFiles(module)
                )
            }
        val host = GeneratedModuleSources(moduleDirectory = plan.host.directory, files = hostFiles(plan))
        return registryOnlyModules + host
    }
}
