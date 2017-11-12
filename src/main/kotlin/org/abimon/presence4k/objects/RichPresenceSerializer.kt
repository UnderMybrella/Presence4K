package org.abimon.presence4k.objects

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import java.time.ZoneOffset

class RichPresenceSerializer: JsonSerializer<RichPresence>() {
    override fun serialize(value: RichPresence, gen: JsonGenerator, serializers: SerializerProvider) {
        val activity: MutableMap<String, Any?> = HashMap()

        activity["state"] = value.state
        activity["details"] = value.details

        if(value.startTimestamp != null || value.endTimestamp != null)
            activity["timestamps"] = mapOf("start" to value.startTimestamp?.toEpochSecond(ZoneOffset.UTC), "end" to value.endTimestamp?.toEpochSecond(ZoneOffset.UTC))

        if(value.largeImageKey != null || value.largeImageText != null || value.smallImageKey != null || value.smallImageText != null)
            activity["assets"] = mapOf(
                    "large_image" to value.largeImageKey,
                    "large_text" to value.largeImageText,
                    "small_image" to value.smallImageKey,
                    "small_text" to value.smallImageText
            )

        if(value.partyID != null || value.partySize != null || value.partyMax != null)
            activity["party"] = mutableMapOf<String, Any?>("id" to value.partyID).apply {
                if(value.partySize != null) {
                    if (value.partyMax ?: 0 > 0)
                        this["size"] = arrayOf(value.partySize, value.partyMax)
                    else
                        this["size"] = arrayOf(value.partySize)
                }
            }

        if(value.matchSecret != null || value.joinSecret != null || value.spectateSecret != null)
            activity["secrets"] = mapOf(
                    "match" to value.matchSecret,
                    "join" to value.joinSecret,
                    "spectate" to value.spectateSecret
            )

        activity["instance"] = true

        gen.writeObject(activity)
    }
}