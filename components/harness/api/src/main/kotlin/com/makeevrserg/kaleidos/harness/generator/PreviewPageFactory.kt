package com.makeevrserg.kaleidos.harness.generator

import com.makeevrserg.kaleidos.harness.HarnessLayout
import com.makeevrserg.kaleidos.harness.HarnessPlan
import com.makeevrserg.kaleidos.harness.PreviewPagePath
import com.makeevrserg.kaleidos.host.DevServerKind

/**
 * The files of the preview application in the host module: the page and the way its dev server reaches
 * it. Kobweb routes to the page, a plain Kotlin/JS module serves it as a file of its own, next to the
 * bundle, so an `index.html` the module already has keeps working.
 *
 * Generated Kotlin quotes a lot of Kotlin, which is what the escapes below are.
 */
@Suppress("StringShouldBeRawString")
class PreviewPageFactory(
    private val naming: HarnessNaming,
    private val pageSourceFactory: PreviewPageSourceFactory
) {

    private fun kotlinFile(fileName: String, lines: List<String>): GeneratedFile {
        val directory = naming.packageDirectory(HarnessNaming.PAGE_PACKAGE)
        return GeneratedFile(
            relativePath = "${HarnessLayout.KOTLIN_DIRECTORY}/$directory/$fileName",
            content = lines.joinToString(separator = "\n", postfix = "\n")
        )
    }

    private fun webpackEntryPointFile(): GeneratedFile {
        val lines = listOf(
            HarnessNaming.HEADER,
            "package ${HarnessNaming.PAGE_PACKAGE}",
            "",
            "import org.jetbrains.compose.web.renderComposable",
            "",
            "private const val ROOT_ELEMENT_ID = \"root\"",
            "",
            "public fun main() {",
            "    renderComposable(rootElementId = ROOT_ELEMENT_ID) {",
            "        PreviewPage()",
            "    }",
            "}"
        )
        return kotlinFile(WEBPACK_ENTRY_POINT_FILE_NAME, lines)
    }

    private fun kobwebRouteFile(): GeneratedFile {
        val lines = listOf(
            HarnessNaming.HEADER,
            "package ${HarnessNaming.PAGE_PACKAGE}",
            "",
            "import androidx.compose.runtime.Composable",
            "import com.varabyte.kobweb.core.Page",
            "",
            "@Page(\"${PreviewPagePath.KOBWEB_ROUTE}\")",
            "@Composable",
            "public fun KaleidosRoute() {",
            "    PreviewPage()",
            "}"
        )
        return kotlinFile(KOBWEB_ROUTE_FILE_NAME, lines)
    }

    /** Webpack serves whatever the resources of the module contain; the page needs a host document. */
    private fun webpackDocumentFile(): GeneratedFile {
        val lines = listOf(
            "<!DOCTYPE html>",
            "<html lang=\"en\">",
            "<head>",
            "    <meta charset=\"utf-8\">",
            "    <title>${PreviewPagePath.TITLE}</title>",
            "    <style>body { margin: 0; }</style>",
            "</head>",
            "<body>",
            "<div id=\"root\"></div>",
            "<script src=\"${PreviewPagePath.WEBPACK_BUNDLE_NAME}\"></script>",
            "</body>",
            "</html>"
        )
        return GeneratedFile(
            relativePath = "${HarnessLayout.RESOURCES_DIRECTORY}/${PreviewPagePath.WEBPACK_FILE_NAME}",
            content = lines.joinToString(separator = "\n", postfix = "\n")
        )
    }

    fun create(plan: HarnessPlan): List<GeneratedFile> {
        val page = kotlinFile(PAGE_FILE_NAME, pageSourceFactory.create(plan))
        return when (plan.host.kind) {
            DevServerKind.KOBWEB -> listOf(page, kobwebRouteFile())
            DevServerKind.WEBPACK -> listOf(page, webpackEntryPointFile(), webpackDocumentFile())
        }
    }

    private companion object {
        const val PAGE_FILE_NAME = "PreviewPage.kt"
        const val WEBPACK_ENTRY_POINT_FILE_NAME = "PreviewMain.kt"
        const val KOBWEB_ROUTE_FILE_NAME = "KaleidosRoute.kt"
    }
}
