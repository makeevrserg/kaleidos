package com.makeevrserg.kaleidos.server.api.fake

import com.makeevrserg.kaleidos.server.api.api.PortListenerCommand

/** Runs whatever the test wants run, and reads whatever the test wants read out of it. */
class FakePortListenerCommand(
    private val arguments: List<String>,
    private val parsedPid: Long?
) : PortListenerCommand {

    override fun arguments(port: Int): List<String> = arguments

    override fun parsePid(output: String, port: Int): Long? = parsedPid

    override fun stopCommand(pid: Long): String = "stop $pid"
}
