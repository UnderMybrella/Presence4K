package org.abimon.presence4k.objects

data class IPCResponse(
        val op: Opcode,
        val payload: IPCPayload?,
        val error: IPCError?
)