package com.a307.linkcare.sdk.health.data.serializer

import com.google.gson.*
import com.samsung.android.sdk.health.data.device.DeviceType
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Gson TypeAdapters for Samsung Health SDK types
 */

// DeviceType serializer
class DeviceTypeSerializer : JsonSerializer<DeviceType> {
    override fun serialize(src: DeviceType?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.toString() ?: "UNKNOWN")
    }
}

// ZoneOffset serializer
class ZoneOffsetSerializer : JsonSerializer<ZoneOffset> {
    override fun serialize(src: ZoneOffset?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.totalSeconds ?: 0)
    }
}

// LocalDateTime serializer
class LocalDateTimeSerializer : JsonSerializer<LocalDateTime> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override fun serialize(src: LocalDateTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.format(formatter))
    }
}

class LocalDateTimeDeserializer : JsonDeserializer<LocalDateTime> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalDateTime {
        return LocalDateTime.parse(json?.asString, formatter)
    }
}
