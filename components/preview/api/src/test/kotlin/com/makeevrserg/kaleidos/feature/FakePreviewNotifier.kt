package com.makeevrserg.kaleidos.feature

import com.makeevrserg.kaleidos.notification.PreviewNotifier

class FakePreviewNotifier : PreviewNotifier {
    val errors = mutableListOf<String>()

    override fun error(content: String) {
        errors += content
    }
}
