package com.makeevrserg.kaleidos.harness.generator.util

import com.makeevrserg.kaleidos.harness.generator.model.GeneratedFile
import com.makeevrserg.kaleidos.harness.model.HarnessPlan
import com.makeevrserg.kaleidos.harness.util.KOBWEB_ROUTE
import com.makeevrserg.kaleidos.harness.util.KOTLIN_DIRECTORY
import com.makeevrserg.kaleidos.harness.util.RESOURCES_DIRECTORY
import com.makeevrserg.kaleidos.harness.util.TITLE
import com.makeevrserg.kaleidos.harness.util.WEBPACK_BUNDLE_NAME
import com.makeevrserg.kaleidos.harness.util.WEBPACK_FILE_NAME
import com.makeevrserg.kaleidos.host.model.DevServerKind

private const val PAGE_FILE_NAME = "PreviewPage.kt"
private const val WEBPACK_ENTRY_POINT_FILE_NAME = "PreviewMain.kt"
private const val KOBWEB_ROUTE_FILE_NAME = "KaleidosRoute.kt"

private fun kotlinFile(fileName: String, lines: List<String>): GeneratedFile {
    val directory = packageDirectory(PAGE_PACKAGE)
    return GeneratedFile(
        relativePath = "$KOTLIN_DIRECTORY/$directory/$fileName",
        content = lines.joinToString(separator = "\n", postfix = "\n")
    )
}

private fun webpackEntryPointFile(): GeneratedFile {
    val lines = listOf(
        HEADER,
        "package $PAGE_PACKAGE",
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
        HEADER,
        "package $PAGE_PACKAGE",
        "",
        "import androidx.compose.runtime.Composable",
        "import com.varabyte.kobweb.core.Page",
        "",
        "@Page(\"$KOBWEB_ROUTE\")",
        "@Composable",
        "public fun KaleidosRoute() {",
        "    PreviewPage()",
        "}"
    )
    return kotlinFile(KOBWEB_ROUTE_FILE_NAME, lines)
}

private fun webpackDocumentFile(): GeneratedFile {
    val lines = listOf(
        "<!DOCTYPE html>",
        "<html lang=\"en\">",
        "<head>",
        "    <meta charset=\"utf-8\">",
        "    <title>$TITLE</title>",
        "    <style>body { margin: 0; }</style>",
        "</head>",
        "<body>",
        "<div id=\"root\"></div>",
        "<script src=\"$WEBPACK_BUNDLE_NAME\"></script>",
        "</body>",
        "</html>"
    )
    return GeneratedFile(
        relativePath = "$RESOURCES_DIRECTORY/$WEBPACK_FILE_NAME",
        content = lines.joinToString(separator = "\n", postfix = "\n")
    )
}

@Suppress("StringShouldBeRawString")
internal fun HarnessPlan.generatePreviewPage(): List<GeneratedFile> {
    val page = kotlinFile(PAGE_FILE_NAME, generatePreviewPageSource())
    return when (host.kind) {
        DevServerKind.KOBWEB -> listOf(page, kobwebRouteFile())
        DevServerKind.WEBPACK -> listOf(
            page,
            webpackEntryPointFile(),
            webpackDocumentFile()
        )
    }
}
