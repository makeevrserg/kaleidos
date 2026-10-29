package com.makeevrserg.kaleidos.harness.generator.util

import com.makeevrserg.kaleidos.harness.HarnessPreview
import com.makeevrserg.kaleidos.harness.PreviewModulePlan
import com.makeevrserg.kaleidos.harness.PrivatePreviewFile
import com.makeevrserg.kaleidos.harness.generator.GeneratedFile
import com.makeevrserg.kaleidos.harness.generator.HarnessNaming

private fun HarnessPreview.wrapperLines(naming: HarnessNaming): List<String> {
    return listOf(
        "",
        "@androidx.compose.runtime.Composable",
        "internal fun ${naming.privatePreviewWrapperName(this)}() {",
        "    ${naming.functionName(this)}()",
        "}"
    )
}

private fun PrivatePreviewFile.toGeneratedFile(
    previews: List<HarnessPreview>,
    naming: HarnessNaming
): GeneratedFile {
    val wrappers = previews
        .filter { preview -> preview.isPrivate && preview.sourcePath == sourcePath }
        .flatMap { preview -> preview.wrapperLines(naming) }
    val lines = listOf(HarnessNaming.HEADER, text.trimEnd()) + wrappers
    return GeneratedFile(
        relativePath = naming.privatePreviewFileCopyPath(sourcePath),
        content = lines.joinToString(separator = "\n", postfix = "\n")
    )
}

internal fun generatePrivatePreviewFiles(module: PreviewModulePlan, naming: HarnessNaming): List<GeneratedFile> {
    return module.privatePreviewFiles.map { file -> file.toGeneratedFile(module.previews, naming) }
}
