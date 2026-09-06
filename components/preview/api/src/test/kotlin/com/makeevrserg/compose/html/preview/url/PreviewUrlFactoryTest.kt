package com.makeevrserg.compose.html.preview.url

import com.makeevrserg.compose.html.preview.feature.PreviewFixtures.previewFunction
import com.makeevrserg.compose.html.preview.feature.PreviewFixtures.target
import kotlin.test.Test
import kotlin.test.assertEquals

class PreviewUrlFactoryTest {
    private val factory = PreviewUrlFactory()

    @Test
    fun GIVEN_single_preview_without_focus_WHEN_create_THEN_preview_parameter_only() {
        val url = factory.create("http://localhost:8085", target(previews = listOf(previewFunction("CardPreview"))))

        assertEquals("http://localhost:8085/?preview=app.CardPreview", url)
    }

    @Test
    fun GIVEN_several_previews_WHEN_create_THEN_all_fqns_in_source_order_separated_by_comma() {
        val previews = listOf(previewFunction("CardPreview"), previewFunction("DarkCardPreview"))

        val url = factory.create("http://localhost:8085", target(previews = previews))

        assertEquals("http://localhost:8085/?preview=app.CardPreview,app.DarkCardPreview", url)
    }

    @Test
    fun GIVEN_focused_preview_WHEN_create_THEN_fragment_points_to_it() {
        val previews = listOf(previewFunction("CardPreview"), previewFunction("DarkCardPreview"))

        val focused = target(previews = previews, focusedFqn = "app.DarkCardPreview")

        val url = factory.create("http://localhost:8085", focused)

        assertEquals("http://localhost:8085/?preview=app.CardPreview,app.DarkCardPreview#app.DarkCardPreview", url)
    }

    @Test
    fun GIVEN_base_url_with_trailing_slash_WHEN_create_THEN_no_double_slash() {
        val url = factory.create("http://localhost:8085/", target())

        assertEquals("http://localhost:8085/?preview=app.CardPreview", url)
    }

    @Test
    fun GIVEN_fqn_with_characters_unsafe_in_urls_WHEN_create_THEN_encoded() {
        val preview = previewFunction("CardPreview").copy(fqn = "app.`Card & Co`")

        val focused = target(previews = listOf(preview), focusedFqn = preview.fqn)

        val url = factory.create("http://localhost:8085", focused)

        assertEquals("http://localhost:8085/?preview=app.%60Card+%26+Co%60#app.%60Card+%26+Co%60", url)
    }
}
