package com.makeevrserg.compose.html.preview.feature

class FakePreviewToolWindowPresenter : PreviewToolWindowPresenter {
    var showCount = 0

    override fun show() {
        showCount++
    }
}
