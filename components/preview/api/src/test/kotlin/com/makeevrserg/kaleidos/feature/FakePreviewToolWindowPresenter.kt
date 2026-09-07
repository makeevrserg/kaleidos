package com.makeevrserg.kaleidos.feature

class FakePreviewToolWindowPresenter : PreviewToolWindowPresenter {
    var showCount = 0

    override fun show() {
        showCount++
    }
}
