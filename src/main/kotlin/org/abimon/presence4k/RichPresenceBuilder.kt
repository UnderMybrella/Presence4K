package org.abimon.presence4k

import org.abimon.presence4k.objects.RichPresence
import java.time.LocalDateTime

class RichPresenceBuilder {
    var state: String? = null
    var details: String? = null
    var startTimestamp: LocalDateTime? = null
    var endTimestamp: LocalDateTime? = null
    var largeImageKey: String? = null
    var largeImageText: String? = null
    var smallImageKey: String? = null
    var smallImageText: String? = null
    var partyID: String? = null
    var partySize: Int? = null
    var partyMax: Int? = null
    var matchSecret: String? = null
    var joinSecret: String? = null
    var spectateSecret: String? = null

    fun withState(state: String?): RichPresenceBuilder {
        this.state = state
        return this
    }

    fun build(): RichPresence = RichPresence(
            state, details, startTimestamp, endTimestamp,
            largeImageKey, largeImageText, smallImageKey,
            smallImageText, partyID, partySize, partyMax,
            matchSecret, joinSecret, spectateSecret
    )
}

fun richPresence(init: RichPresenceBuilder.() -> Unit): RichPresence {
    val builder = RichPresenceBuilder()
    builder.init()
    return builder.build()
}