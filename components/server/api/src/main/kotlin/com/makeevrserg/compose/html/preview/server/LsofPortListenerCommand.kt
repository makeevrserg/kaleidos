package com.makeevrserg.compose.html.preview.server

/**
 * `lsof -t` prints process ids and nothing else, one per line, already filtered down to the listening
 * socket of the port. `-n -P` keeps it from resolving host and service names, which is what makes it
 * slow and, on an unreachable mount, hang.
 */
class LsofPortListenerCommand : PortListenerCommand {

    override fun arguments(port: Int): List<String> {
        return listOf("lsof", "-nP", "-iTCP:$port", "-sTCP:LISTEN", "-t")
    }

    /** Forked processes share a listening socket, and any of them names the program well enough. */
    override fun parsePid(output: String, port: Int): Long? {
        return output.lineSequence().firstNotNullOfOrNull { line -> line.trim().toLongOrNull() }
    }

    override fun stopCommand(pid: Long): String = "kill $pid"
}
