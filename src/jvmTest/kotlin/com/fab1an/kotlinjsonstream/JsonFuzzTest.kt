package com.fab1an.kotlinjsonstream

import com.code_intelligence.jazzer.junit.FuzzTest
import okio.Buffer

class JsonFuzzTest {

    @FuzzTest
    fun intFuzzing(int: Int) {

        val buffer = Buffer()
        JsonWriter(buffer).apply {
            beginArray()
            value(int)
            endArray()
        }

        JsonReader(buffer.readUtf8()).apply {
            beginArray()
            nextInt() shouldEqual int
            endArray()
        }
    }

    @FuzzTest
    fun doubleFuzzing(double: Double) {
        if (double.isNaN() || double.isInfinite()) return

        val buffer = Buffer()
        JsonWriter(buffer).apply {
            beginArray()
            value(double)
            endArray()
        }

        JsonReader(buffer.readUtf8()).apply {
            beginArray()
            nextDouble() shouldEqual double
            endArray()
        }
    }

    @FuzzTest
    fun longFuzzing(long: Long) {
        val buffer = Buffer()
        JsonWriter(buffer).apply {
            beginArray()
            value(long)
            endArray()
        }

        JsonReader(buffer.readUtf8()).apply {
            beginArray()
            nextLong() shouldEqual long
            endArray()
        }
    }

    @FuzzTest
    fun stringFuzzing(text: String?) {
        if (text == null) return

        val buffer = Buffer()
        JsonWriter(buffer).apply {
            beginArray()
            value(text)
            endArray()
        }

        JsonReader(buffer.readUtf8()).apply {
            beginArray()
            nextString() shouldEqual text
            endArray()
        }
    }

    @FuzzTest
    fun nameFuzzing(text: String?) {
        if (text == null) return

        val buffer = Buffer()
        JsonWriter(buffer).apply {
            beginObject()
            name(text)
            value(text)
            endObject()
        }

        JsonReader(buffer.readUtf8()).apply {
            beginObject()
            nextName() shouldEqual text
            nextString() shouldEqual text
            endObject()
        }
    }
}
