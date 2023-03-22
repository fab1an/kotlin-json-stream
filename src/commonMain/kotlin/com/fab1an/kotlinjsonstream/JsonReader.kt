package com.fab1an.kotlinjsonstream

import okio.BufferedSource
import okio.ByteString

class JsonReader(private val source: BufferedSource) {

    private val stack = mutableListOf<OpenToken>()

    init {
        consumeWhitespace()
    }

    fun beginArray() {
        check(stack.lastOrNull() in setOf(OpenToken.BEGIN_ARRAY, OpenToken.BEGIN_PROPERTY, null)) {
            stack.lastOrNull() ?: "empty"
        }
        if (stack.lastOrNull() == OpenToken.BEGIN_PROPERTY) {
            stack.removeLast()
        }
        stack.add(OpenToken.BEGIN_ARRAY)

        consumeSpecific('[')
        consumeWhitespace()
    }

    fun beginObject() {
        check(stack.lastOrNull() in setOf(OpenToken.BEGIN_ARRAY, OpenToken.BEGIN_PROPERTY, null)) {
            stack.lastOrNull() ?: "empty"
        }
        if (stack.lastOrNull() == OpenToken.BEGIN_PROPERTY) {
            stack.removeLast()
        }
        stack.add(OpenToken.BEGIN_OBJECT)

        consumeSpecific('{')
        consumeWhitespace()
    }

    fun close() {
        source.close()
    }

    fun endArray() {
        check(stack.lastOrNull() == OpenToken.BEGIN_ARRAY) {
            stack.lastOrNull() ?: "empty"
        }
        stack.removeLast()

        consumeSpecific(']')
        consumeWhitespaceAndOptionalComma()
    }

    fun endObject() {
        check(stack.lastOrNull() == OpenToken.BEGIN_OBJECT) {
            stack.lastOrNull() ?: "empty"
        }
        stack.removeLast()

        consumeSpecific('}')
        consumeWhitespaceAndOptionalComma()
    }

    fun hasNext(): Boolean {
        return when (stack.lastOrNull()) {
            OpenToken.BEGIN_OBJECT -> peek() != JsonToken.END_OBJECT
            OpenToken.BEGIN_ARRAY -> peek() != JsonToken.END_ARRAY
            OpenToken.BEGIN_PROPERTY, null -> error("not inside array/object")
        }
    }

    fun nextBoolean(): Boolean {
        expectValue()

        /* value */
        val boolVal = when {
            source.nextIs("true") -> {
                source.skip(4)
                true
            }

            source.nextIs("false") -> {
                source.skip(5)
                false
            }

            // tochars
            else -> error("not a boolean: '${source.readUtf8CodePoint().toChar()}'")
        }

        /* whitespace and comma */
        consumeWhitespaceAndOptionalComma()

        return boolVal
    }

    fun nextDouble(): Double {
        expectValue()

        /* first part value */
        val partBeforeComma = source.readDecimalLong()

        var partAfterComma: Long? = null
        if (source.nextIs('.')) {
            source.skip(1)
            partAfterComma = source.readDecimalLong()
        }

        /* whitespace and comma */
        consumeWhitespaceAndOptionalComma()

        if (partAfterComma == null) {
            return partBeforeComma.toDouble()

        } else {
            return "$partBeforeComma.$partAfterComma".toDouble()
        }
    }

    fun nextInt(): Int {
        expectValue()

        /* int value */
        val value = source.readDecimalLong()

        /* whitespace and comma */
        consumeWhitespaceAndOptionalComma()

        return value.toInt()
    }

    fun nextLong(): Long {
        expectValue()

        /* int value */
        val value = source.readDecimalLong()

        /* whitespace and comma */
        consumeWhitespaceAndOptionalComma()

        return value
    }

    fun nextName(): String {
        check(stack.lastOrNull() == OpenToken.BEGIN_OBJECT) {
            stack.lastOrNull() ?: "empty"
        }
        stack.add(OpenToken.BEGIN_PROPERTY)

        /* opening quote */
        consumeSpecific('"')

        /* property name */
        val indexOfEnd = source.indexOf(ByteString.of('"'.code.toByte()))
        val name = source.readUtf8(indexOfEnd)

        /* closing quote */
        consumeSpecific('"')

        /* colon and whitespace */
        consumeWhitespace()
        consumeSpecific(':')
        consumeWhitespace()

        return name
    }

    fun nextNull() {
        check(peek() == JsonToken.NULL)
        source.skip(4)
    }

    fun nextString(): String {
        expectValue()

        /* opening quote */
        consumeSpecific('"')

        /* string value */
        val value = StringBuilder()
        while (!source.exhausted()) {
            if (source.nextIs("""\"""")) {
                source.skip(2)
                value.append('"')

            } else if (source.nextIs("""\\""")) {
                source.skip(2)
                value.append('\\')

            } else if (!source.nextIs('"')) {
                value.append(source.readUtf8CodePoint().toChar())

            } else {
                break
            }
        }

        /* closing quote */
        consumeSpecific('"')

        /* whitespace and comma */
        consumeWhitespaceAndOptionalComma()

        return value.toString()
    }

    fun peek(): JsonToken {
        return when {
            source.exhausted() -> JsonToken.END_DOCUMENT
            stack.lastOrNull() in setOf(OpenToken.BEGIN_OBJECT) && source.nextIs('"') -> JsonToken.NAME
            source.nextIs('"') -> JsonToken.STRING
            source.nextIs('{') -> JsonToken.BEGIN_OBJECT
            source.nextIs('}') -> JsonToken.END_OBJECT
            source.nextIs('[') -> JsonToken.BEGIN_ARRAY
            source.nextIs(']') -> JsonToken.END_ARRAY
            source.nextIs("null") -> JsonToken.NULL
            source.nextIs("true") -> JsonToken.BOOLEAN
            source.nextIs("false") -> JsonToken.BOOLEAN
            source.nextIsAsciiDigit() -> JsonToken.NUMBER
            else -> error("unknown next token: '${source.readUtf8CodePoint().toChar()}'")
        }
    }

    fun skipValue() {
        check(stack.lastOrNull() in setOf(OpenToken.BEGIN_PROPERTY, OpenToken.BEGIN_ARRAY)) {
            stack.lastOrNull() ?: "empty"
        }

        if (peek() == JsonToken.NULL) {
            nextNull()

            if (stack.lastOrNull() == OpenToken.BEGIN_PROPERTY) {
                stack.removeLast()
            }

            /* whitespace and comma */
            consumeWhitespaceAndOptionalComma()

        } else if (peek() == JsonToken.BOOLEAN) {
            nextBoolean()

        } else if (peek() == JsonToken.STRING) {
            nextString()

        } else if (peek() == JsonToken.BEGIN_OBJECT) {
            beginObject()
            while (hasNext()) {
                nextName()
                skipValue()
            }
            endObject()

        } else if (peek() == JsonToken.BEGIN_ARRAY) {
            beginArray()
            while (hasNext()) {
                skipValue()
            }
            endArray()

        } else {
            nextDouble()
        }
    }

    private fun expectValue() {
        check(stack.lastOrNull() in setOf(OpenToken.BEGIN_PROPERTY, OpenToken.BEGIN_ARRAY)) {
            stack.lastOrNull() ?: "empty stack"
        }
        if (stack.lastOrNull() == OpenToken.BEGIN_PROPERTY) {
            stack.removeLast()
        }
    }

    private fun consumeWhitespaceAndOptionalComma() {

        /* whitespace and comma */
        consumeWhitespace()
        if (source.nextIs(',')) {
            source.skip(1)
            consumeWhitespace()

        } else {

            if (stack.lastOrNull() == OpenToken.BEGIN_ARRAY) {
                check(peek() == JsonToken.END_ARRAY) { "']' expected" }
            } else if (stack.lastOrNull() == OpenToken.BEGIN_OBJECT) {
                check(peek() == JsonToken.END_OBJECT) { "'}' expected" }
            }
        }
    }

    private fun consumeWhitespace() {
        while (!source.exhausted() && source.nextIsJsonWhiteSpace()) {
            source.skip(1)
        }
    }

    private fun consumeSpecific(char: Char) {
        check(!source.exhausted()) { "source is exhausted" }
        check(source.nextIs(char)) {
            "'$char' expected, but got: '${source.readUtf8CodePoint().toChar()}'"
        }
        source.readUtf8CodePoint()
    }

    private enum class OpenToken {
        BEGIN_OBJECT,
        BEGIN_ARRAY,
        BEGIN_PROPERTY
    }
}
