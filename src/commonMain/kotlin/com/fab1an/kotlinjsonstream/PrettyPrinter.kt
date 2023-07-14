package com.fab1an.kotlinjsonstream

import com.fab1an.kotlinjsonstream.JsonToken.BEGIN_ARRAY
import com.fab1an.kotlinjsonstream.JsonToken.BEGIN_OBJECT
import com.fab1an.kotlinjsonstream.JsonToken.BOOLEAN
import com.fab1an.kotlinjsonstream.JsonToken.END_ARRAY
import com.fab1an.kotlinjsonstream.JsonToken.END_DOCUMENT
import com.fab1an.kotlinjsonstream.JsonToken.END_OBJECT
import com.fab1an.kotlinjsonstream.JsonToken.NAME
import com.fab1an.kotlinjsonstream.JsonToken.NULL
import com.fab1an.kotlinjsonstream.JsonToken.NUMBER
import com.fab1an.kotlinjsonstream.JsonToken.STRING
import okio.Buffer

/**
 * Convenience function for pretty-printing a json-String.
 *
 * @param jsonStr the String to be pretty-printed.
 */
fun prettyPrintJson(jsonStr: String): String {
    val sink = Buffer()
    val writer = JsonWriter(sink, prettyPrint = true)

    val reader = JsonReader(jsonStr)
    while (reader.peek() != END_DOCUMENT) {
        reader.apply {
            when (peek()) {
                BEGIN_ARRAY -> {
                    beginArray()
                    writer.beginArray()
                }

                BEGIN_OBJECT -> {
                    beginObject()
                    writer.beginObject()
                }

                BOOLEAN -> writer.value(nextBoolean())
                END_ARRAY -> {
                    endArray()
                    writer.endArray()
                }

                END_DOCUMENT -> {
                }

                END_OBJECT -> {
                    endObject()
                    writer.endObject()
                }

                NAME -> writer.name(nextName())
                NULL -> writer.nullValue()
                NUMBER -> writer.value(nextDouble())
                STRING -> writer.value(nextString())
            }
        }
    }

    return sink.readUtf8()
}
