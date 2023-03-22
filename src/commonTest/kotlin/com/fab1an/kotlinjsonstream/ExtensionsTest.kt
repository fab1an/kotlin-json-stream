package com.fab1an.kotlinjsonstream

import okio.Buffer
import kotlin.test.Test

class ExtensionsTest {

    @Test
    fun valueList() {
        val intList = listOf(1, 2, 3)

        val buffer = Buffer()

        val writer = JsonWriter(buffer)
        writer.value(intList, JsonWriter::value)
        buffer.readUtf8() shouldEqual """
            [1,2,3]
        """.trimIndent()
    }

    @Test
    fun nextList() {
        val json = """[1,2,3,4,5]"""
        val reader = JsonReader(json)

        reader.nextList(JsonReader::nextInt) shouldEqual listOf(1, 2, 3, 4, 5)
    }

    @Test
    fun valueSet() {
        val intSet = setOf(1, 2, 3)
        val buffer = Buffer()
        val writer = JsonWriter(buffer)
        writer.value(intSet, JsonWriter::value)
        buffer.readUtf8() shouldEqual """
            [1,2,3]
        """.trimIndent()
    }

    @Test
    fun nextSet() {
        val json = """[1,2,2,2,3]"""
        val reader = JsonReader(json)

        reader.nextSet(JsonReader::nextInt) shouldEqual setOf(1, 2, 3)
    }

    @Test
    fun nextOrNullTest() {
        val json = """{"a": 2, "b": null}"""
        JsonReader(json).apply {
            beginObject()

            nextName()
            nextOrNull(JsonReader::nextInt) shouldEqual 2

            nextName()
            nextOrNull(JsonReader::nextInt) shouldEqual null

            endObject()
        }
    }
}
