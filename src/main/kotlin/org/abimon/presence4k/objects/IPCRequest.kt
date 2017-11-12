package org.abimon.presence4k.objects

data class IPCRequest(
        val op: Opcode,
        val data: Any
)