package com.makeevrserg.kaleidos.harness.api.generator.util

import com.makeevrserg.kaleidos.harness.api.generator.model.GeneratedFile
import com.makeevrserg.kaleidos.harness.api.model.HarnessPreview
import com.makeevrserg.kaleidos.harness.api.model.PreviewModulePlan
import com.makeevrserg.kaleidos.harness.api.model.PrivatePreviewFile

private fun HarnessPreview.wrapperLines(): List<String> {
    return listOf(
        "",
        "@androidx.compose.runtime.Composable",
        "internal fun ${privatePreviewWrapperName()}() {",
        "    ${functionName()}()",
        "}"
    )
}

private fun PrivatePreviewFile.toGeneratedFile(previews: List<HarnessPreview>): GeneratedFile {
    val wrappers = previews
        .filter { preview -> preview.isPrivate && preview.sourcePath == sourcePath }
        .flatMap(HarnessPreview::wrapperLines)
    val lines = listOf(HEADER, text.trimEnd()) + wrappers
    return GeneratedFile(
        relativePath = privatePreviewFileCopyPath(sourcePath),
        content = lines.joinToString(separator = "\n", postfix = "\n")
    )
}

internal fun PreviewModulePlan.generatePrivatePreviewFiles(): List<GeneratedFile> {
    return privatePreviewFiles.map { file -> file.toGeneratedFile(previews) }
}
