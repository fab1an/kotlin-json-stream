package com.fab1an.kotlinjsonstream

import com.fab1an.kotlinjsonstream.JsonReader.OpenToken.BEGIN_ARRAY
import com.fab1an.kotlinjsonstream.JsonReader.OpenToken.BEGIN_OBJECT
import com.fab1an.kotlinjsonstream.JsonReader.OpenToken.BEGIN_PROPERTY
import com.fab1an.kotlinjsonstream.JsonReader.OpenToken.CONTINUE_ARRAY
import com.fab1an.kotlinjsonstream.JsonReader.OpenToken.CONTINUE_OBJECT
import okio.Buffer
import okio.BufferedSource
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8

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
        Buffer().apply { writeUtf8(jsonStr) }
    )

    private val stack = mutableListOf<OpenToken>()

    /**
     * Consumes the next token from the JSON stream and asserts that it is the beginning of a new array.
     */
    fun beginArray() {
        skipWhitespaceAndOptionalComma()
        expectValue()
        stack.add(BEGIN_ARRAY)

        skipSpecific(BYTESTRING_SQUAREBRACKET_OPEN)
    }

    /**
     * Consumes the next token from the JSON stream and asserts that it is the beginning of a new object.
     */
    fun beginObject() {
        skipWhitespaceAndOptionalComma()
        expectValue()
        stack.add(BEGIN_OBJECT)

        skipSpecific(BYTESTRING_CURLYBRACKET_OPEN)
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
        when (stack.lastOrNull()) {
            BEGIN_ARRAY, CONTINUE_ARRAY -> stack.removeLast()
            else -> error("stack is ${stack.lastOrNull()}")
        }

        source.skipWhitespace()
        skipSpecific(BYTESTRING_SQUAREBRACKET_CLOSE)
    }

    /**
     * Consumes the next token from the JSON stream and asserts that it is the end of the current object.
     */
    fun endObject() {
        when (stack.lastOrNull()) {
            BEGIN_OBJECT, CONTINUE_OBJECT -> stack.removeLast()
            else -> error("stack is ${stack.lastOrNull()}")
        }

        source.skipWhitespace()
        skipSpecific(BYTESTRING_CURLYBRACKET_CLOSE)
    }

    /**
     * Returns true if the current array or object has another element
     */
    fun hasNext(): Boolean {
        source.skipWhitespace()
        return when (stack.lastOrNull()) {
            CONTINUE_OBJECT, CONTINUE_ARRAY -> source.nextIs(BYTESTRING_COMMA)
            BEGIN_OBJECT -> !source.nextIs(BYTESTRING_CURLYBRACKET_CLOSE)
            BEGIN_ARRAY -> !source.nextIs(BYTESTRING_SQUAREBRACKET_CLOSE)
            BEGIN_PROPERTY, null -> error("not inside array/object")
        }
    }

    /**
     * Returns the Boolean value of the next token, consuming it.
     */
    fun nextBoolean(): Boolean {
        expectNotTopLevel()
        skipWhitespaceAndOptionalComma()
        expectValue()

        return source.readJsonBoolean()
    }

    /**
     * Returns the [Double] value of the next token, consuming it.
     */
    fun nextDouble(): Double {
        return nextDouble(skipDouble = false)!!
    }

    private fun nextDouble(skipDouble: Boolean): Double? {
        expectNotTopLevel()
        skipWhitespaceAndOptionalComma()
        expectValue()

        /* first part value */
        val nextStop = source.indexOfElement("}], \t\r\n".encodeUtf8())
        check(nextStop > 0) { "document ended prematurely" }

        val numberText = source.readUtf8(nextStop)
        when {
            numberText.startsWith(".") -> throw NumberFormatException("invalid number: '$numberText'")
            !numberText.all {
                (it == '.' || it == 'E' || it == 'e' || it == '+' || it == '-' || (it in '0'..'9'))
            } -> {
                throw NumberFormatException("invalid number: '$numberText'")
            }

            numberText.startsWith("-00") -> throw NumberFormatException("invalid number: '$numberText'")
            numberText.startsWith("00") -> throw NumberFormatException("invalid number: '$numberText'")
            else -> {
                val doubleValue = numberText.toDouble()
                return if (skipDouble)
                    null
                else
                    doubleValue
            }
        }
    }

    /**
     * Returns the Int value of the next token, consuming it.
     */
    fun nextInt(): Int {
        return nextLong().toInt()
    }

    /**
     * Returns the Long value of the next token, consuming it.
     */
    fun nextLong(): Long {
        val double = nextDouble()
        check(double.rem(1) == 0.0) { "number has fraction part: $double" }
        return double.toLong()
    }

    /**
     * Returns the name of the next property, consuming it and asserting that this reader is inside an object.
     */
    fun nextName(): String {
        return nextName(skipName = false)!!
    }

    /**
     * Returns the name of the next property, consuming it and asserting that this reader is inside an object.
     */
    private fun nextName(skipName: Boolean): String? {
        skipWhitespaceAndOptionalComma()
        check(stack.lastOrNull() == BEGIN_OBJECT) { "stack is ${stack.lastOrNull()}" }

        /* opening quote */
        skipSpecific(BYTESTRING_DOUBLEQUOTE)

        /* property name */
        val indexOfEnd = source.indexOf(BYTESTRING_DOUBLEQUOTE)
        check(indexOfEnd >= 0) { "did not find ending double-quote '\"'" }
        val name: String? =
            if (!skipName) {
                source.readUtf8(indexOfEnd)

            } else {
                source.skip(indexOfEnd)
                null
            }

        /* closing quote */
        skipSpecific(BYTESTRING_DOUBLEQUOTE)

        /* whitespace and colon */
        source.skipWhitespace()
        skipSpecific(BYTESTRING_COLON)

        /* update stack */
        stack.add(BEGIN_PROPERTY)

        return name
    }

    /**
     * Asserts that the next property-value or array-item is null and consumes the token.
     */
    fun nextNull() {
        expectNotTopLevel()
        skipWhitespaceAndOptionalComma()
        expectValue()

        skipSpecific(BYTESTRING_NULL)
    }

    /**
     * Returns the String of the next token, consuming it.
     */
    fun nextString(): String {
        return nextString(skipString = false)!!
    }

    private fun nextString(skipString: Boolean): String? {
        expectNotTopLevel()
        skipWhitespaceAndOptionalComma()
        expectValue()

        /* opening quote */
        skipSpecific(BYTESTRING_DOUBLEQUOTE)

        val buffer: Buffer? = if (skipString) null else Buffer()
        while (true) {

            /* read until backslash or double quote */
            val nextStop = source.indexOfElement(BYTESTRING_BACKSLASH_OR_DOUBLEQUOTE)
            check(nextStop >= 0) { "document ended prematurely" }

            if (buffer != null)
                buffer.write(source, nextStop)
            else
                source.skip(nextStop)

            when {
                source.nextIs(BYTESTRING_ESCAPED_BACKSLASH) -> {
                    source.skip(2)
                    buffer?.write(BYTESTRING_BACKSLASH)
                }

                source.nextIs(BYTESTRING_ESCAPED_DOUBLEQUOTE) -> {
                    source.skip(2)
                    buffer?.write(BYTESTRING_DOUBLEQUOTE)
                }

                source.nextIs(BYTESTRING_ESCAPED_FORWARDSLASH) -> {
                    source.skip(2)
                    buffer?.write(BYTESTRING_FORWARDSLASH)
                }

                source.nextIs(BYTESTRING_ESCAPED_BACKSPACE) -> {
                    source.skip(2)
                    buffer?.write(BYTESTRING_BACKSPACE)
                }

                source.nextIs(BYTESTRING_ESCAPED_FORMFEED) -> {
                    source.skip(2)
                    buffer?.write(BYTESTRING_FORMFEED)
                }

                source.nextIs(BYTESTRING_ESCAPED_NEWLINE) -> {
                    source.skip(2)
                    buffer?.write(BYTESTRING_NEWLINE)
                }

                source.nextIs(BYTESTRING_ESCAPED_CARRIAGERETURN) -> {
                    source.skip(2)
                    buffer?.write(BYTESTRING_CARRIAGERETURN)
                }

                source.nextIs(BYTESTRING_ESCAPED_TAB) -> {
                    source.skip(2)
                    buffer?.write(BYTESTRING_TAB)
                }

                source.nextIs(BYTESTRING_START_OF_UNICODE_ESCAPE) -> {
                    source.skip(2)
                    val charVal = source.readHexadecimalUnsignedLong().toInt().toChar()
                    check(charVal <= Char.MAX_VALUE)
                    buffer?.writeUtf8CodePoint(charVal.code)
                }

                else -> {
                    break
                }
            }
        }

        /* closing quote */
        skipSpecific(BYTESTRING_DOUBLEQUOTE)

        return buffer?.readUtf8()
    }

    /**
     * Returns the next token without consuming it.
     */
    fun peek(): JsonToken {
        skipWhitespaceAndOptionalComma()

        return when {
            source.exhausted() -> JsonToken.END_DOCUMENT
            source.nextIs(BYTESTRING_DOUBLEQUOTE) -> if (stack.lastOrNull() == BEGIN_OBJECT) JsonToken.NAME else JsonToken.STRING
            source.nextIs(BYTESTRING_CURLYBRACKET_OPEN) -> JsonToken.BEGIN_OBJECT
            source.nextIs(BYTESTRING_CURLYBRACKET_CLOSE) -> JsonToken.END_OBJECT
            source.nextIs(BYTESTRING_SQUAREBRACKET_OPEN) -> JsonToken.BEGIN_ARRAY
            source.nextIs(BYTESTRING_SQUAREBRACKET_CLOSE) -> JsonToken.END_ARRAY
            source.nextIs(BYTESTRING_NULL) -> JsonToken.NULL
            source.nextIs(BYTESTRING_TRUE) -> JsonToken.BOOLEAN
            source.nextIs(BYTESTRING_FALSE) -> JsonToken.BOOLEAN
            source.nextIsHyphenOrAsciiDigit() -> JsonToken.NUMBER
            else -> error("unexpected next character: '${source.readUtf8CodePoint().toChar()}'")
        }
    }

    /**
     * Skips the next value recursively. This method asserts it is inside an array or inside an object.
     */
    fun skipValue() {
        when (peek()) {
            JsonToken.BEGIN_ARRAY -> {
                beginArray()
                while (hasNext()) {
                    skipValue()
                }
                endArray()
            }

            JsonToken.BEGIN_OBJECT -> {
                beginObject()
                while (hasNext()) {
                    nextName(skipName = true)
                    skipValue()
                }
                endObject()
            }

            JsonToken.BOOLEAN -> nextBoolean()
            JsonToken.NULL -> nextNull()
            JsonToken.NUMBER -> nextDouble(skipDouble = true)
            JsonToken.STRING -> nextString(skipString = true)

            JsonToken.END_DOCUMENT, JsonToken.END_ARRAY, JsonToken.END_OBJECT, JsonToken.NAME -> error("unexpected next tooken: '${peek()}'")
        }
    }

    private fun expectNotTopLevel() {
        check(stack.isNotEmpty()) { "top-level, call to beginObject() or beginArray() expected" }
    }

    private fun expectValue() {
        when (stack.lastOrNull()) {
            BEGIN_OBJECT, CONTINUE_OBJECT -> error("inside object, call to nextName() expected")
            BEGIN_PROPERTY -> {
                stack.removeLast()
                stack[stack.lastIndex] = CONTINUE_OBJECT
            }

            BEGIN_ARRAY -> stack[stack.lastIndex] = CONTINUE_ARRAY
            CONTINUE_ARRAY -> error("should not happen")
            null -> Unit
        }
    }

    private fun skipWhitespaceAndOptionalComma() {
        source.skipWhitespace()
        if (source.nextIs(BYTESTRING_COMMA)) {
            when (stack.lastOrNull()) {
                CONTINUE_ARRAY -> stack[stack.lastIndex] = BEGIN_ARRAY
                CONTINUE_OBJECT -> stack[stack.lastIndex] = BEGIN_OBJECT
                else -> error("stack is ${stack.lastOrNull()}")
            }

            skipSpecific(BYTESTRING_COMMA)
            source.skipWhitespace()
        }
    }

    private fun skipSpecific(byteString: ByteString) {
        check(!source.exhausted()) {
            "'${byteString.utf8()}' expected, but source is exhausted"
        }
        check(source.nextIs(byteString)) {
            "'${byteString.utf8()}' expected, but got: '${source.readUtf8CodePoint().toChar()}'"
        }
        source.skip(byteString.size.toLong())
    }

    private enum class OpenToken {
        BEGIN_OBJECT,
        CONTINUE_OBJECT,
        BEGIN_ARRAY,
        CONTINUE_ARRAY,
        BEGIN_PROPERTY
    }
}
