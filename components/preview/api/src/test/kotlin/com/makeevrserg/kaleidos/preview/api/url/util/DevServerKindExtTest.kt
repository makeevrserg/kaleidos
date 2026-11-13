@file:Suppress("FunctionNaming")

package com.makeevrserg.kaleidos.preview.api.url.util

import com.makeevrserg.kaleidos.host.api.model.DevServerKind
import com.makeevrserg.kaleidos.preview.api.feature.model.PreviewFixtures.previewFunction
import kotlin.test.Test
import kotlin.test.assertEquals

class DevServerKindExtTest {
    private val cardPreviews = listOf(previewFunction("CardPreview"))

    @Test
    fun GIVEN_single_preview_without_focus_WHEN_create_THEN_preview_parameter_only() {
        val url = DevServerKind.WEBPACK.buildPreviewUrl(
            "http://localhost:8085",
            cardPreviews,
            focusedFqn = null
        )

        assertEquals("http://localhost:8085/kaleidos.html?preview=app.CardPreview", url)
    }

    @Test
    fun GIVEN_several_previews_WHEN_create_THEN_all_fqns_in_source_order_separated_by_comma() {
        val previews = listOf(previewFunction("CardPreview"), previewFunction("DarkCardPreview"))

        val url = DevServerKind.WEBPACK.buildPreviewUrl(
            "http://localhost:8085",
            previews,
            focusedFqn = null
        )

        assertEquals(
            "http://localhost:8085/kaleidos.html?preview=app.CardPreview,app.DarkCardPreview",
            url
        )
    }

    @Test
    fun GIVEN_focused_preview_WHEN_create_THEN_fragment_points_to_it() {
        val previews = listOf(previewFunction("CardPreview"), previewFunction("DarkCardPreview"))

        val url = DevServerKind.WEBPACK.buildPreviewUrl(
            "http://localhost:8085",
            previews,
            "app.DarkCardPreview"
        )

        assertEquals(
            "http://localhost:8085/kaleidos.html?preview=app.CardPreview,app.DarkCardPreview" +
                "#app.DarkCardPreview",
            url
        )
    }

    @Test
    fun GIVEN_base_url_with_trailing_slash_WHEN_create_THEN_no_double_slash() {
        val url = DevServerKind.WEBPACK.buildPreviewUrl(
            "http://localhost:8085/",
            cardPreviews,
            focusedFqn = null
        )

        assertEquals("http://localhost:8085/kaleidos.html?preview=app.CardPreview", url)
    }

    @Test
    fun GIVEN_kobweb_host_WHEN_create_THEN_the_url_points_at_the_generated_route() {
        val url = DevServerKind.KOBWEB.buildPreviewUrl(
            "http://localhost:8086",
            cardPreviews,
            focusedFqn = null
        )

        assertEquals("http://localhost:8086/kaleidos?preview=app.CardPreview", url)
    }

    @Test
    fun GIVEN_fqn_with_characters_unsafe_in_urls_WHEN_create_THEN_encoded() {
        val preview = previewFunction("CardPreview").copy(fqn = "app.`Card & Co`")

        val url = DevServerKind.WEBPACK.buildPreviewUrl(
            "http://localhost:8085",
            listOf(preview),
            preview.fqn
        )

        assertEquals(
            "http://localhost:8085/kaleidos.html?preview=app.%60Card+%26+Co%60#app.%60Card+%26+Co%60",
            url
        )
    }
}
