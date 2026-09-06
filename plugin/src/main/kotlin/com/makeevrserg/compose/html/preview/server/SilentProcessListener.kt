package com.makeevrserg.compose.html.preview.server

/** For runs whose outcome nobody waits for, such as the stop task. */
object SilentProcessListener : DevServerProcessListener {
    override fun onOutput(text: String) = Unit

    override fun onExited(isSuccess: Boolean) = Unit
}
