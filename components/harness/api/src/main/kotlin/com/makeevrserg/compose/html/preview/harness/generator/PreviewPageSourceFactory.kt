package com.makeevrserg.compose.html.preview.harness.generator

import com.makeevrserg.compose.html.preview.harness.HarnessPlan
import com.makeevrserg.compose.html.preview.harness.PreviewPagePath

/**
 * The source of the page itself: it reads the `preview` query parameter, renders every preview it names
 * through the registry of the module that owns it, and scrolls to the fragment the gutter icon puts into
 * the URL. Without a parameter it shows everything the host can reach, which makes the page a gallery.
 *
 * Generated Kotlin quotes a lot of Kotlin, which is what the escapes below are.
 */
@Suppress("StringShouldBeRawString")
class PreviewPageSourceFactory(private val naming: HarnessNaming) {

    private fun registryFqns(plan: HarnessPlan): List<String> {
        return plan.modules.map { module -> naming.registryFqn(module.gradlePath) }
    }

    /** The page keeps no list of its own: every module contributes the previews it compiled in. */
    private fun knownPreviewLines(plan: HarnessPlan): List<String> {
        val registries = registryFqns(plan)
        if (registries.isEmpty()) return listOf("private val allPreviewFqns: List<String> = emptyList()")
        return buildList {
            add("private val allPreviewFqns: List<String> =")
            registries.forEachIndexed { index, registry ->
                val separator = if (index == registries.lastIndex) "" else " +"
                add("    $registry.fqns$separator")
            }
        }
    }

    private fun headerLines(plan: HarnessPlan): List<String> {
        return buildList {
            add(HarnessNaming.HEADER)
            add("package ${HarnessNaming.PAGE_PACKAGE}")
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
            add("private const val PAGE_TITLE = \"${PreviewPagePath.TITLE}\"")
            add("")
            addAll(knownPreviewLines(plan))
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

    private fun renderLines(plan: HarnessPlan): List<String> {
        val dispatch = registryFqns(plan).flatMap { registry ->
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

    /**
     * One preview is a card of its own: a header naming it and a body with room around the composable,
     * so several previews of a file do not run into each other.
     */
    private fun sectionLines(): List<String> {
        return listOf(
            "private const val BORDER = \"1px solid rgba(127, 127, 127, 0.35)\"",
            "",
            "@Composable",
            "private fun PreviewSection(fqn: String) {",
            "    Div(attrs = {",
            "        id(fqn)",
            "        style {",
            "            property(\"margin\", \"16px\")",
            "            property(\"border\", BORDER)",
            "            property(\"border-radius\", \"8px\")",
            "            property(\"overflow\", \"hidden\")",
            "        }",
            "    }) {",
            "        Div(attrs = {",
            "            style {",
            "                property(\"font\", \"12px monospace\")",
            "                property(\"opacity\", \"0.6\")",
            "                property(\"padding\", \"8px 12px\")",
            "                property(\"border-bottom\", BORDER)",
            "            }",
            "        }) {",
            "            Text(fqn.substringAfterLast('.'))",
            "        }",
            "        Div(attrs = { style { property(\"padding\", \"16px\") } }) {",
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

    fun create(plan: HarnessPlan): List<String> {
        return buildList {
            addAll(headerLines(plan))
            add("")
            addAll(requestLines())
            add("")
            addAll(renderLines(plan))
            add("")
            addAll(sectionLines())
            add("")
            addAll(entryLines())
        }
    }
}
