package com.makeevrserg.kaleidos.harness.generator.util

import com.makeevrserg.kaleidos.harness.model.HarnessPlan
import com.makeevrserg.kaleidos.harness.util.TITLE

private fun HarnessPlan.registryFqns(): List<String> {
    return modules.map { module -> registryFqn(module.gradlePath) }
}

private fun HarnessPlan.knownPreviewLines(): List<String> {
    val registries = registryFqns()
    if (registries.isEmpty()) return listOf("private val allPreviewFqns: List<String> = emptyList()")
    return buildList {
        add("private val allPreviewFqns: List<String> =")
        registries.forEachIndexed { index, registry ->
            val separator = if (index == registries.lastIndex) "" else " +"
            add("    $registry.fqns$separator")
        }
    }
}

private fun HarnessPlan.headerLines(): List<String> {
    return buildList {
        add(HEADER)
        add("package $PAGE_PACKAGE")
        add("")
        add("import androidx.compose.runtime.Composable")
        add("import androidx.compose.runtime.DisposableEffect")
        add("import kotlinx.browser.document")
        add("import kotlinx.browser.window")
        add("import org.jetbrains.compose.web.dom.Div")
        add("import org.jetbrains.compose.web.dom.Text")
        add("import org.w3c.dom.url.URLSearchParams")
        add("")
        add("private const val PREVIEW_PARAMETER = \"preview\"")
        add("private const val FQN_SEPARATOR = \",\"")
        add("private const val PAGE_TITLE = \"$TITLE\"")
        add("")
        addAll(knownPreviewLines())
    }
}

private fun requestLines(): List<String> {
    return listOf(
        "private fun requestedFqns(): List<String> {",
        "    val requested = URLSearchParams(window.location.search).get(PREVIEW_PARAMETER).orEmpty()",
        "    return requested.split(FQN_SEPARATOR)",
        "        .map { fqn -> fqn.trim() }",
        "        .filter { fqn -> fqn.isNotEmpty() }",
        "}"
    )
}

private fun HarnessPlan.renderLines(): List<String> {
    val dispatch = registryFqns().flatMap { registry ->
        listOf(
            "    if (fqn in $registry.fqns) {",
            "        $registry.Render(fqn)",
            "        return",
            "    }"
        )
    }
    return buildList {
        add("@Composable")
        add("private fun RenderPreview(fqn: String) {")
        addAll(dispatch)
        add(
            "    Text(\"Preview \$fqn is not in this build of the page: the dev server has not " +
                "compiled it yet. Press Restart Dev Server if it stays that way.\")"
        )
        add("}")
    }
}

private fun sectionLines(): List<String> {
    return listOf(
        "private const val BORDER = \"1px solid rgba(127, 127, 127, 0.35)\"",
        "",
        "@Composable",
        "private fun PreviewSection(fqn: String) {",
        "    Div(attrs = {",
        "        id(fqn)",
        "        style {",
        """            property("margin", "16px")""",
        "            property(\"border\", BORDER)",
        """            property("border-radius", "8px")""",
        """            property("overflow", "hidden")""",
        "        }",
        "    }) {",
        "        Div(attrs = {",
        "            style {",
        """                property("font", "12px monospace")""",
        """                property("opacity", "0.6")""",
        """                property("padding", "8px 12px")""",
        "                property(\"border-bottom\", BORDER)",
        "            }",
        "        }) {",
        "            Text(fqn.substringAfterLast('.'))",
        "        }",
        """        Div(attrs = { style { property("padding", "16px") } }) {""",
        "            RenderPreview(fqn)",
        "        }",
        "    }",
        "}"
    )
}

private fun entryLines(): List<String> {
    return listOf(
        "/** The previews the URL asks for; every known preview when it asks for none. */",
        "@Composable",
        "public fun PreviewPage() {",
        "    val fqns = requestedFqns().ifEmpty { allPreviewFqns }",
        "    DisposableEffect(fqns) {",
        "        // Tells the IDE this really is the generated page and not the site the dev server",
        "        // answers with for a path its bundle does not know.",
        "        document.title = PAGE_TITLE",
        "        val anchor = window.location.hash.removePrefix(\"#\")",
        "        if (anchor.isNotEmpty()) document.getElementById(anchor)?.scrollIntoView()",
        "        onDispose { }",
        "    }",
        "    fqns.forEach { fqn -> PreviewSection(fqn) }",
        "}"
    )
}

@Suppress("StringShouldBeRawString")
internal fun HarnessPlan.generatePreviewPageSource(): List<String> {
    return buildList {
        addAll(headerLines())
        add("")
        addAll(requestLines())
        add("")
        addAll(renderLines())
        add("")
        addAll(sectionLines())
        add("")
        addAll(entryLines())
    }
}
