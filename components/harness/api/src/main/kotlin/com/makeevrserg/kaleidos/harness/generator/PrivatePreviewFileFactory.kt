package com.makeevrserg.kaleidos.harness.generator

import com.makeevrserg.kaleidos.harness.HarnessPreview
import com.makeevrserg.kaleidos.harness.PreviewModulePlan
import com.makeevrserg.kaleidos.harness.PrivatePreviewFile

class PrivatePreviewFileFactory(private val naming: HarnessNaming) {

    private fun wrapperLines(preview: HarnessPreview): List<String> {
        return listOf(
            "",
            "@androidx.compose.runtime.Composable",
            "internal fun ${naming.privatePreviewWrapperName(preview)}() {",
            "    ${naming.functionName(preview)}()",
            "}"
        )
    }

    private fun copyOf(file: PrivatePreviewFile, previews: List<HarnessPreview>): GeneratedFile {
        val wrappers = previews
            .filter { preview -> preview.isPrivate && preview.sourcePath == file.sourcePath }
            .flatMap(::wrapperLines)
        val lines = listOf(HarnessNaming.HEADER, file.text.trimEnd()) + wrappers
        return GeneratedFile(
            relativePath = naming.privatePreviewFileCopyPath(file.sourcePath),
            content = lines.joinToString(separator = "\n", postfix = "\n")
        )
    }

    fun create(module: PreviewModulePlan): List<GeneratedFile> {
        return module.privatePreviewFiles.map { file -> copyOf(file, module.previews) }
    }
}
