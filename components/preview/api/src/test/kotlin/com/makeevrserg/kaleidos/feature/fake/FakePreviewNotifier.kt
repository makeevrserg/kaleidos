package com.makeevrserg.kaleidos.feature.fake

import com.makeevrserg.kaleidos.notification.api.PreviewNotifier

class FakePreviewNotifier : PreviewNotifier {
    val errors = mutableListOf<String>()

    override fun error(content: String) {
        errors += content
    }
}
