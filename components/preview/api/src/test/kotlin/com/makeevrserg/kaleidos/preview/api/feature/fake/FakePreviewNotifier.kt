package com.makeevrserg.kaleidos.preview.api.feature.fake

import com.makeevrserg.kaleidos.preview.api.notification.api.PreviewNotifier

class FakePreviewNotifier : PreviewNotifier {
    val errors = mutableListOf<String>()

    override fun error(content: String) {
        errors += content
    }
}
