package com.fab1an.kotlinjsonstream

import okio.Buffer
import okio.BufferedSource
import okio.ByteString

/**
 * Reads a JSON (<a href="http://www.ietf.org/rfc/rfc7159.txt">RFC 7159</a>)
 * encoded value as a stream of tokens. This stream includes both literal
 * values (strings, numbers, booleans, and nulls) as well as the begin and
 * end delimiters of objects and arrays. The tokens are traversed in
 * depth-first order, the same order that they appear in the JSON document.
 *
 * @constructor Creates a new instance that reads a JSON-encoded stream from [source].
 */
class JsonReader(private val source: BufferedSource) {

    /**
     * Creates a new instance that reads a JSON-encoded stream from [jsonStr].
     */
    constructor(jsonStr: String) : this(
        Buffer().apply { write(jsonStr.encodeToByteArray()) }
    )

    private val stack = mutableListOf<OpenToken>()

    init {
        consumeWhitespace()
    }

    /**
     * Consumes the next token from the JSON stream and asserts that it is the beginning of a new array.
     */
    fun beginArray() {
        beginArrayOrObject()
        stack.add(OpenToken.BEGIN_ARRAY)

        consumeSpecific(BYTESTRING_SQUAREBRACKET_OPEN)
        consumeWhitespace()
    }

    private fun beginArrayOrObject() {
        check(stack.lastOrNull().let {
            it == OpenToken.BEGIN_ARRAY
                    || it == OpenToken.BEGIN_PROPERTY
                    || it == null
        }) {
            stack.lastOrNull() ?: "empty"
        }
        if (stack.lastOrNull() == OpenToken.BEGIN_PROPERTY) {
            stack.removeLast()
        }
    }


    /**
     * Consumes the next token from the JSON stream and asserts that it is the beginning of a new object.
     */
    fun beginObject() {
        beginArrayOrObject()
        stack.add(OpenToken.BEGIN_OBJECT)

        consumeSpecific(BYTESTRING_CURLYBRACKET_OPEN)
        consumeWhitespace()
    }

    /**
     * Closes the underlying source.
     */
    fun close() {
        source.close()
    }

    /**
     * Consumes the next token from the JSON stream and asserts that it is the end of the current array.
     */
    fun endArray() {
        check(stack.lastOrNull() == OpenToken.BEGIN_ARRAY) {
            stack.lastOrNull() ?: "empty"
        }
        stack.removeLast()

        consumeSpecific(BYTESTRING_SQUAREBRACKET_CLOSE)
        consumeWhitespaceAndOptionalComma()
    }

    /**
     * Consumes the next token from the JSON stream and asserts that it is the end of the current object.
     */
    fun endObject() {
        check(stack.lastOrNull() == OpenToken.BEGIN_OBJECT) {
            stack.lastOrNull() ?: "empty"
        }
        stack.removeLast()

        consumeSpecific(BYTESTRING_CURLYBRACKET_CLOSE)
        consumeWhitespaceAndOptionalComma()
    }

    /**
     * Returns true if the current array or object has another element
     */
    fun hasNext(): Boolean {
        return when (stack.lastOrNull()) {
            OpenToken.BEGIN_OBJECT -> peek() != JsonToken.END_OBJECT
            OpenToken.BEGIN_ARRAY -> peek() != JsonToken.END_ARRAY
            OpenToken.BEGIN_PROPERTY, null -> error("not inside array/object")
        }
    }

    /**
     * Returns the Boolean value of the next token, consuming it.
     */
    fun nextBoolean(): Boolean {
        expectValue()

        /* value */
        val boolVal = when {
            source.nextIs(BYTESTRING_TRUE) -> {
                source.skip(4)
                true
            }

            source.nextIs(BYTESTRING_FALSE) -> {
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

    /**
     * Returns the [Double] value of the next token, consuming it.
     */
    fun nextDouble(): Double {
        expectValue()

        /* first part value */
        val partBeforeComma = source.readDecimalLong()

        var partAfterComma: Long? = null
        if (source.nextIs(BYTESTRING_DOT)) {
            source.skip(1)
            partAfterComma = source.readDecimalLong()
        }

        /* whitespace and comma */
        consumeWhitespaceAndOptionalComma()

        return if (partAfterComma == null) {
            partBeforeComma.toDouble()

        } else {
            "$partBeforeComma.$partAfterComma".toDouble()
        }
    }

    /**
     * Returns the Int value of the next token, consuming it.
     */
    fun nextInt(): Int {
        expectValue()

        /* int value */
        val value = source.readDecimalLong()

        /* whitespace and comma */
        consumeWhitespaceAndOptionalComma()

        return value.toInt()
    }

    /**
     * Returns the Long value of the next token, consuming it.
     */
    fun nextLong(): Long {
        expectValue()

        /* int value */
        val value = source.readDecimalLong()

        /* whitespace and comma */
        consumeWhitespaceAndOptionalComma()

        return value
    }

    /**
     * Returns the name of the next property, consuming it and asserting that this reader is inside an object.
     */
    fun nextName(): String {
        check(stack.lastOrNull() == OpenToken.BEGIN_OBJECT) {
            stack.lastOrNull() ?: "empty"
        }
        stack.add(OpenToken.BEGIN_PROPERTY)

        /* opening quote */
        consumeSpecific(BYTESTRING_DOUBLEQUOTE)

        /* property name */
        val indexOfEnd = source.indexOf(BYTESTRING_DOUBLEQUOTE)
        val name = source.readUtf8(indexOfEnd)

        /* closing quote */
        consumeSpecific(BYTESTRING_DOUBLEQUOTE)

        /* colon and whitespace */
        consumeWhitespace()
        consumeSpecific(BYTESTRING_COLON)
        consumeWhitespace()

        return name
    }

    /**
     * Asserts that the next property-value or array-item is null and consumes the token.
     */
    fun nextNull() {
        check(peek() == JsonToken.NULL)
        source.skip(4)
    }

    /**
     * Returns the String of the next token, consuming it.
     */
    fun nextString(): String {
        expectValue()

        /* opening quote */
        consumeSpecific(BYTESTRING_DOUBLEQUOTE)

        /* string value */
        val value = StringBuilder()
        while (!source.exhausted()) {
            if (source.nextIs(BYTESTRING_ESCAPED_DOUBLEQUOTE)) {
                source.skip(2)
                value.append('"')

            } else if (source.nextIs(BYTESTRING_ESCAPED_FORWARD_SLASH)) {
                source.skip(2)
                value.append('\\')

            } else if (!source.nextIs(BYTESTRING_DOUBLEQUOTE)) {
                value.append(source.readUtf8CodePoint().toChar())

            } else {
                break
            }
        }

        /* closing quote */
        consumeSpecific(BYTESTRING_DOUBLEQUOTE)

        /* whitespace and comma */
        consumeWhitespaceAndOptionalComma()

        return value.toString()
    }

    /**
     * Returns the next token without consuming it.
     */
    fun peek(): JsonToken {
        return when {
            source.exhausted() -> JsonToken.END_DOCUMENT
            stack.lastOrNull() == OpenToken.BEGIN_OBJECT && source.nextIs(BYTESTRING_DOUBLEQUOTE) -> JsonToken.NAME
            source.nextIs(BYTESTRING_DOUBLEQUOTE) -> JsonToken.STRING
            source.nextIs(BYTESTRING_CURLYBRACKET_OPEN) -> JsonToken.BEGIN_OBJECT
            source.nextIs(BYTESTRING_CURLYBRACKET_CLOSE) -> JsonToken.END_OBJECT
            source.nextIs(BYTESTRING_SQUAREBRACKET_OPEN) -> JsonToken.BEGIN_ARRAY
            source.nextIs(BYTESTRING_SQUAREBRACKET_CLOSE) -> JsonToken.END_ARRAY
            source.nextIs(BYTESTRING_NULL) -> JsonToken.NULL
            source.nextIs(BYTESTRING_TRUE) -> JsonToken.BOOLEAN
            source.nextIs(BYTESTRING_FALSE) -> JsonToken.BOOLEAN
            source.nextIsAsciiDigit() -> JsonToken.NUMBER
            else -> error("unknown next token: '${source.readUtf8CodePoint().toChar()}'")
        }
    }

    /**
     * Skips the next value recursively. This method asserts it is inside an array or inside an object.
     */
    fun skipValue() {
        check(stack.lastOrNull().let {
            it == OpenToken.BEGIN_ARRAY
                    || it == OpenToken.BEGIN_PROPERTY
        }) {
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
        check(stack.lastOrNull().let {
            it == OpenToken.BEGIN_ARRAY
                    || it == OpenToken.BEGIN_PROPERTY
        }) {
            stack.lastOrNull() ?: "empty stack"
        }
        if (stack.lastOrNull() == OpenToken.BEGIN_PROPERTY) {
            stack.removeLast()
        }
    }

    private fun consumeWhitespaceAndOptionalComma() {

        /* whitespace and comma */
        consumeWhitespace()
        if (source.nextIs(BYTESTRING_COMMA)) {
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

    private fun consumeSpecific(byteString: ByteString) {
        check(!source.exhausted()) {
            "'${byteString.utf8()}' expected, source is exhausted"
        }
        check(source.nextIs(byteString)) {
            "'${byteString.utf8()}' expected, but got: '${source.readUtf8CodePoint().toChar()}'"
        }
        source.skip(1)
    }

    private enum class OpenToken {
        BEGIN_OBJECT,
        BEGIN_ARRAY,
        BEGIN_PROPERTY
    }
}
