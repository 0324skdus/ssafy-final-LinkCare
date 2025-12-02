package com.ssafy.sdk.health.data.adapter

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.time.ZoneOffset

class ZoneOffsetAdapter : TypeAdapter<ZoneOffset>() {
    override fun write(out: JsonWriter, value: ZoneOffset?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value.id)
        }
    }

    override fun read(`in`: JsonReader): ZoneOffset? {
        return if (`in`.peek() == com.google.gson.stream.JsonToken.NULL) {
            `in`.nextNull()
            null
        } else {
            ZoneOffset.of(`in`.nextString())
        }
    }
}