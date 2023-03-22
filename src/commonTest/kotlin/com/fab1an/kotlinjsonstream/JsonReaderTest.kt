package com.fab1an.kotlinjsonstream

import kotlin.test.Test
import kotlin.test.assertFailsWith

class JsonReaderTest {

    @Test
    fun readJson() {
        val json = JsonReader("""{"stringProp":"string", "intProp":0}""")

        json.beginObject()
        json.nextName() shouldEqual "stringProp"
        json.nextString() shouldEqual "string"
        json.nextName() shouldEqual "intProp"
        json.nextInt() shouldEqual 0
        json.endObject()
    }

    @Test
    fun readDouble() {
        val json = JsonReader("""{"doubleProp":1.0}""")

        json.beginObject()
        json.nextName() shouldEqual "doubleProp"
        json.nextDouble() shouldEqual 1.0
        json.endObject()
    }

    @Test
    fun readBoolean() {
        var json = JsonReader("""{"boolProp":true}""")

        json.beginObject()
        json.nextName() shouldEqual "boolProp"
        json.nextBoolean() shouldEqual true
        json.endObject()

        json = JsonReader("""{"boolProp":false}""")
        json.beginObject()
        json.nextName() shouldEqual "boolProp"
        json.nextBoolean() shouldEqual false
        json.endObject()
    }

    @Test
    fun readMixedNestedArray() {
        val json = JsonReader("""{  "array": [1, "zwei", 3, "vier"]  }""")

        json.beginObject()
        json.nextName() shouldEqual "array"
        json.beginArray()
        json.nextInt() shouldEqual 1
        json.nextString() shouldEqual "zwei"
        json.nextInt() shouldEqual 3
        json.nextString() shouldEqual "vier"
        json.endArray()
        json.endObject()
    }

    @Test
    fun readNestedObject() {
        val json = JsonReader("""{  "obj": {"value": 1}  }""")

        json.beginObject()
        json.nextName() shouldEqual "obj"
        json.beginObject()
        json.nextName() shouldEqual "value"
        json.nextInt() shouldEqual 1
        json.endObject()
        json.endObject()
    }

    @Test
    fun errorNoCommaInArray() {
        val json = JsonReader("""[1 "zwei"]""")

        json.beginArray()
        assertFailsWith<IllegalStateException> {
            json.nextInt() shouldEqual 1
            json.nextString() shouldEqual "zwei"
        }
    }

    @Test
    fun errorMismatchedBrackets() {
        val json = JsonReader("""{"test": [}]""")

        json.beginObject()
        json.nextName() shouldEqual "test"
        json.beginArray()
        assertFailsWith<IllegalStateException> {
            json.endArray()
        }
    }

    @Test
    fun errorValueInsideObject() {
        val json = JsonReader("""{5}""")

        json.beginObject()
        assertFailsWith<IllegalStateException> {
            json.nextName() shouldEqual 5
        }
    }

    @Test
    fun hasNextArray() {
        var json = JsonReader("""[]""")

        json.beginArray()
        json.hasNext() shouldEqual false
        json.endArray()

        json = JsonReader("""[1,2]""")

        json.beginArray()
        json.hasNext() shouldEqual true
        json.nextInt()
        json.hasNext() shouldEqual true
        json.nextInt()
        json.hasNext() shouldEqual false
        json.endArray()
    }

    @Test
    fun hasNextObject() {
        var json = JsonReader("""{}""")

        assertFailsWith<IllegalStateException> { json.hasNext() }

        json = JsonReader("""{}""")

        json.beginObject()
        json.hasNext() shouldEqual false
        json.endObject()

        json = JsonReader("""{"a":"", "b":""}""")

        json.beginObject()
        json.hasNext() shouldEqual true
        json.nextName()
        json.nextString()
        json.hasNext() shouldEqual true
        json.nextName()
        json.nextString()
        json.hasNext() shouldEqual false
        json.endObject()
    }

    @Test
    fun checkNextNull() {
        JsonReader("""{"temp": null}""").apply {
            beginObject()
            nextName() shouldEqual "temp"
            peek() shouldEqual JsonToken.NULL
            skipValue()
            endObject()
        }

        JsonReader("""[1,null,3]""").apply {
            beginArray()
            peek() shouldEqual JsonToken.NUMBER
            nextInt() shouldEqual 1
            peek() shouldEqual JsonToken.NULL
            skipValue()
            peek() shouldEqual JsonToken.NUMBER
            nextInt() shouldEqual 3
            endArray()
        }
    }

    @Test
    fun skipBoolValue() {
        val json = JsonReader("""{"val1": true, "val2": false, "val3": "string"}""")

        json.beginObject()
        json.nextName() shouldEqual "val1"
        json.skipValue()
        json.nextName() shouldEqual "val2"
        json.skipValue()
        json.nextName() shouldEqual "val3"
        json.nextString() shouldEqual "string"
        json.endObject()
    }

    @Test
    fun skipDoubleValue() {
        val json = JsonReader("""[1.0]""")

        json.beginArray()
        json.skipValue()
        json.endArray()
    }

    @Test
    fun skipObject() {
        val json = JsonReader("""{"val1": {"innerVal1": ""}, "val2": "b"}""")

        json.beginObject()
        json.nextName() shouldEqual "val1"
        json.skipValue()
        json.nextName() shouldEqual "val2"
        json.nextString() shouldEqual "b"
        json.endObject()
    }

    @Test
    fun skipArrayValue() {
        val json = JsonReader("""[2,"b",3]""")

        json.beginArray()
        json.nextInt() shouldEqual 2
        json.skipValue()
        json.nextInt() shouldEqual 3
        json.endArray()
    }

    @Test
    fun escapedString() {
        val json = JsonReader("""[ "Escaped \" Character" ]""")

        json.beginArray()
        json.nextString() shouldEqual "Escaped \" Character"
        json.endArray()
    }

    @Test
    fun readBackslashesInString() {
        val json = JsonReader(""" { "title": "C:\\PROGRA~1\\" } """)

        json.beginObject()
        json.nextName() shouldEqual "title"
        json.nextString() shouldEqual """C:\PROGRA~1\"""
        json.endObject()
    }
}
