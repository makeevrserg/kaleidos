package com.makeevrserg.kaleidos.harness.generator.util

import com.makeevrserg.kaleidos.harness.generator.model.GeneratedFile
import com.makeevrserg.kaleidos.harness.model.HarnessPreview
import com.makeevrserg.kaleidos.harness.model.PreviewModulePlan
import com.makeevrserg.kaleidos.harness.model.PrivatePreviewFile

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
