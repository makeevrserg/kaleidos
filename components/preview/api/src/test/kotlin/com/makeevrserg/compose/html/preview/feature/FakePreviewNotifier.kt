package com.makeevrserg.compose.html.preview.feature

import com.makeevrserg.compose.html.preview.notification.PreviewNotifier

class FakePreviewNotifier : PreviewNotifier {
    val errors = mutableListOf<String>()

    override fun error(content: String) {
        errors += content
    }
}
