package com.makeevrserg.compose.html.preview.server

/**
 * How an operating system is asked which process listens on a TCP port, and how its answer reads.
 */
interface PortListenerCommand {
    /** Command to run, the executable first. */
    fun arguments(port: Int): List<String>

    /** Process id in the output of that command; null when nothing there listens on [port]. */
    fun parsePid(output: String, port: Int): Long?

    /** What the user types to end the process, in the shell of this operating system. */
    fun stopCommand(pid: Long): String
}
