package com.fab1an.kotlinjsonstream

import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertFailsWith

class JsonWriterTest {

    @Test
    fun writeJson() {
        val buffer = Buffer()
        JsonWriter(buffer).apply {
            beginObject()
            name("name").value("value")
            name("inner").beginArray()
            value("item1")
            endArray()
            endObject()
        }

        buffer.readUtf8() shouldEqual """
            {"name":"value","inner":["item1"]}
        """.trimIndent()
    }

    @Test
    fun writeNull() {
        val buffer = Buffer()

        JsonWriter(buffer).apply {

            beginObject()
            name("name").nullValue()
            endObject()

        }

        buffer.readUtf8() shouldEqual """
            {"name":null}
        """.trimIndent()
    }

    @Test
    fun valueDirectlyInObject() {
        assertFailsWith<IllegalStateException> {
            val buffer = Buffer()

            JsonWriter(buffer).apply {
                beginObject()
                value("")
            }
        }
    }

    @Test
    fun quoteInScript() {
        val buffer = Buffer()

        JsonWriter(buffer).apply {
            beginObject()
            name("string").value("a \" quote")
            endObject()
        }

        buffer.readUtf8() shouldEqual """
            {"string":"a \" quote"}
        """.trimIndent()
    }

    @Test
    fun escapeString() {
        val stringWithEscapeSequences = "Escaped: \",\\,/,\b,\u000c,\n,\r,\t,\u0000,\uFFFF"

        val buffer = Buffer()
        JsonWriter(buffer).apply {
            beginArray()
            value(stringWithEscapeSequences)
            endArray()
        }

        val jsonString = buffer.readUtf8()
        jsonString shouldEqual """["Escaped: \",\\,\/,\b,\f,\n,\r,\t,${"\u0000"},${"\uFFFF"}"]"""

        JsonReader(jsonString).apply {
            beginArray()
            nextString() shouldEqual stringWithEscapeSequences
            endArray()
        }
    }

    @Test
    fun writeDoubleZero() {
        val buffer = Buffer()

        JsonWriter(buffer).apply {
            beginArray()
            value(0.0)
            endArray()
        }

        buffer.readUtf8() shouldEqual """
            [0]
        """.trimIndent()
    }

    @Test
    fun writeLong() {
        val buffer = Buffer()

        JsonWriter(buffer).apply {
            beginArray()
            value(1L)
            endArray()
        }

        buffer.readUtf8() shouldEqual """
            [1]
        """.trimIndent()
    }
}
