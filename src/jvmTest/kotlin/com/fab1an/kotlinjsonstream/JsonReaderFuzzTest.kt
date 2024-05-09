package com.fab1an.kotlinjsonstream

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import okio.Buffer

class JsonReaderFuzzTest {

    @FuzzTest
    fun intFuzzing(data: FuzzedDataProvider) {
        val int = data.consumeInt()

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
    fun stringFuzzing(data: FuzzedDataProvider) {
        val text = data.consumeRemainingAsString()

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
}
