package com.makeevrserg.kaleidos.harness.api.generator.util

import com.makeevrserg.kaleidos.harness.api.generator.model.GeneratedFile
import com.makeevrserg.kaleidos.harness.api.generator.model.GeneratedModuleSources
import com.makeevrserg.kaleidos.harness.api.model.HarnessPlan
import com.makeevrserg.kaleidos.harness.api.model.PreviewModulePlan

private fun PreviewModulePlan.moduleFiles(): List<GeneratedFile> {
    return listOf(generatePreviewRegistry()) + generatePrivatePreviewFiles()
}

private fun HarnessPlan.hostFiles(): List<GeneratedFile> {
    val moduleFiles = modules
        .filter { module -> module.directory == host.directory }
        .flatMap(PreviewModulePlan::moduleFiles)
    return moduleFiles + generatePreviewPage() + generatePreviewInitScript()
}

internal fun HarnessPlan.generateHarnessSources(): List<GeneratedModuleSources> {
    val registryOnlyModules = modules
        .filterNot { module -> module.directory == host.directory }
        .map { module ->
            GeneratedModuleSources(
                moduleDirectory = module.directory,
                files = module.moduleFiles()
            )
        }
    val host = GeneratedModuleSources(moduleDirectory = host.directory, files = hostFiles())
    return registryOnlyModules + host
}
