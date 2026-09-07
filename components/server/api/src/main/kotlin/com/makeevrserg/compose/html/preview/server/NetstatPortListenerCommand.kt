package com.makeevrserg.compose.html.preview.server

/**
 * Windows has no `lsof`. `netstat -ano` prints one socket per line, with the process id last:
 * `TCP    0.0.0.0:8080    0.0.0.0:0    LISTENING    4242`.
 */
class NetstatPortListenerCommand : PortListenerCommand {

    override fun arguments(port: Int): List<String> = listOf("netstat", "-ano", "-p", "TCP")

    override fun parsePid(output: String, port: Int): Long? {
        return output.lineSequence()
            .map { line -> line.trim().split(WHITESPACE) }
            .filter { columns -> columns.size >= COLUMN_COUNT }
            .filter { columns -> columns[STATE_COLUMN].equals(LISTENING_STATE, ignoreCase = true) }
            .filter { columns -> columns[LOCAL_ADDRESS_COLUMN].endsWith("$PORT_SEPARATOR$port") }
            .firstNotNullOfOrNull { columns -> columns[PID_COLUMN].toLongOrNull() }
    }

    override fun stopCommand(pid: Long): String = "taskkill /PID $pid /F"

    private companion object {
        const val COLUMN_COUNT = 5
        const val LOCAL_ADDRESS_COLUMN = 1
        const val STATE_COLUMN = 3
        const val PID_COLUMN = 4
        const val LISTENING_STATE = "LISTENING"
        const val PORT_SEPARATOR = ":"
        val WHITESPACE = Regex("""\s+""")
    }
}
