package org.abimon.presence4k.objects

data class IPCPayload(
        val cmd: EnumPayloadCommand,
        val nonce: String?,
        val evt: EnumEvent?,
        val data: Map<String, Any?>?
)