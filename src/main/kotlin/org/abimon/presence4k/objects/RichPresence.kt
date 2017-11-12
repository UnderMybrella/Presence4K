package org.abimon.presence4k.objects

import java.time.LocalDateTime

data class RichPresence(
        val state: String?,
        val details: String?,
        val startTimestamp: LocalDateTime?,
        val endTimestamp: LocalDateTime?,
        val largeImageKey: String?,
        val largeImageText: String?,
        val smallImageKey: String?,
        val smallImageText: String?,
        val partyID: String?,
        val partySize: Int?,
        val partyMax: Int?,
        val matchSecret: String?,
        val joinSecret: String?,
        val spectateSecret: String?
)