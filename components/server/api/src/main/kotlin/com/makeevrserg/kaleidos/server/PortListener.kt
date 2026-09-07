package com.makeevrserg.kaleidos.server

/**
 * The process that holds a TCP port of this machine.
 *
 * @param commandLine what started it, as the operating system reports it; null when it does not hand
 * the command line out, for a process of another user for example
 * @param stopCommand what the user types to end it, in the shell of this operating system
 */
data class PortListener(
    val pid: Long,
    val commandLine: String?,
    val stopCommand: String
)
