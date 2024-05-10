package com.fab1an.kotlinjsonstream

import com.fab1an.kotlinjsonstream.JsonReader.OpenToken.BEGIN_ARRAY
import com.fab1an.kotlinjsonstream.JsonReader.OpenToken.BEGIN_OBJECT
import com.fab1an.kotlinjsonstream.JsonReader.OpenToken.BEGIN_PROPERTY
import com.fab1an.kotlinjsonstream.JsonReader.OpenToken.CONTINUE_ARRAY
import com.fab1an.kotlinjsonstream.JsonReader.OpenToken.CONTINUE_OBJECT
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
        Buffer().apply { writeUtf8(jsonStr) }
    )

    private val stack = mutableListOf<OpenToken>()
    private val sharedBuffer = Buffer()

    /**
     * Consumes the next token from the JSON stream and asserts that it is the beginning of a new array.
     */
    fun beginArray() {
        skipWhitespaceAndOptionalCommaAndUpdateStack()
        expectValueAndUpdateStack()
        stack.add(BEGIN_ARRAY)

        skipSpecific(BYTESTRING_SQUAREBRACKET_OPEN)
    }

    /**
     * Consumes the next token from the JSON stream and asserts that it is the beginning of a new object.
     */
    fun beginObject() {
        skipWhitespaceAndOptionalCommaAndUpdateStack()
        expectValueAndUpdateStack()
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
        skipWhitespaceAndOptionalCommaAndUpdateStack()
        expectValueAndUpdateStack()

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
        skipWhitespaceAndOptionalCommaAndUpdateStack()
        expectValueAndUpdateStack()

        /* first part value */
        parseDoubleIntoBuffer()
        val doubleValue = sharedBuffer.readUtf8()
        sharedBuffer.clear()
        return if (skipDouble)
            null
        else
            doubleValue.toDouble()
    }

    /**
     * See https://www.json.org/json-en.html
     */
    private fun parseDoubleIntoBuffer() {
        check(sharedBuffer.size == 0L) { "sharedBuffer is not empty" }

        var state = 0
        var finished = false
        var numberFormatError = false

        while (!finished) {
            when (state) {
                0 -> {
                    /* start of number */
                    when {
                        source.nextIs(BYTESTRING_HYPHEN) -> {
                            state = 1
                            source.read(sharedBuffer, 1)
                        }

                        source.nextIs(BYTESTRING_ZERO) -> {
                            state = 2
                            source.read(sharedBuffer, 1)
                        }

                        source.nextIsAsciiDigitExceptZero() -> {
                            state = 3
                            source.read(sharedBuffer, 1)
                        }

                        else -> {
                            numberFormatError = true
                            finished = true
                        }
                    }
                }

                1 -> {
                    /* after leading hyphen */
                    when {
                        source.nextIs(BYTESTRING_ZERO) -> {
                            state = 2
                            source.read(sharedBuffer, 1)
                        }

                        source.nextIsAsciiDigitExceptZero() -> {
                            state = 3
                            source.read(sharedBuffer, 1)
                        }

                        else -> {
                            numberFormatError = true
                            finished = true
                        }
                    }
                }

                2 -> {
                    /* before optional fraction and exponent */
                    when {
                        source.nextIs(BYTESTRING_DOT) -> {
                            state = 4
                            source.read(sharedBuffer, 1)
                        }

                        source.nextIs(BYTESTRING_LOWERCASE_E) -> {
                            state = 6
                            source.read(sharedBuffer, 1)
                        }

                        source.nextIs(BYTESTRING_UPPERCASE_E) -> {
                            state = 6
                            source.read(sharedBuffer, 1)
                        }

                        else -> {
                            finished = true
                        }
                    }
                }

                3 -> {
                    /* number before fraction or exponent */
                    when {
                        source.nextIsAsciiDigit() -> {
                            state = 3
                            source.read(sharedBuffer, 1)
                        }

                        source.nextIs(BYTESTRING_DOT) -> {
                            state = 4
                            source.read(sharedBuffer, 1)
                        }

                        source.nextIs(BYTESTRING_LOWERCASE_E) -> {
                            state = 6
                            source.read(sharedBuffer, 1)
                        }

                        source.nextIs(BYTESTRING_UPPERCASE_E) -> {
                            state = 6
                            source.read(sharedBuffer, 1)
                        }

                        else -> {
                            finished = true
                        }
                    }
                }

                4 -> {
                    /* number after comma */
                    when {
                        source.nextIsAsciiDigit() -> {
                            state = 5
                            source.read(sharedBuffer, 1)
                        }

                        else -> {
                            numberFormatError = true
                            finished = true
                        }
                    }
                }

                5 -> {
                    /* number after comma */
                    when {
                        source.nextIsAsciiDigit() -> {
                            state = 5
                            source.read(sharedBuffer, 1)
                        }

                        source.nextIs(BYTESTRING_LOWERCASE_E) -> {
                            state = 6
                            source.read(sharedBuffer, 1)
                        }

                        source.nextIs(BYTESTRING_UPPERCASE_E) -> {
                            state = 6
                            source.read(sharedBuffer, 1)
                        }

                        else -> {
                            finished = true
                        }
                    }
                }

                6 -> {
                    /* after e or E for exponent */
                    when {
                        source.nextIs(BYTESTRING_HYPHEN) -> {
                            state = 7
                            source.read(sharedBuffer, 1)
                        }

                        source.nextIs(BYTESTRING_PLUS) -> {
                            state = 7
                            source.skip(1)
                        }

                        source.nextIsAsciiDigit() -> {
                            state = 7
                            source.read(sharedBuffer, 1)
                        }

                        else -> {
                            numberFormatError = true
                            finished = true
                        }
                    }
                }

                7 -> {
                    /* number in exponent */
                    when {
                        source.nextIsAsciiDigit() -> {
                            state = 7
                            source.read(sharedBuffer, 1)
                        }

                        else -> {
                            finished = true
                        }
                    }
                }
            }
        }

        when (stack.lastOrNull()) {
            BEGIN_PROPERTY -> error("inside object, call to nextName() expected")
            BEGIN_OBJECT, CONTINUE_OBJECT -> {
                if (!source.nextIsWhitespace() && !source.nextIs(BYTESTRING_CURLYBRACKET_CLOSE) && !source.nextIs(
                        BYTESTRING_COMMA
                    )
                ) {
                    numberFormatError = true
                }
            }

            BEGIN_ARRAY, CONTINUE_ARRAY -> {
                if (!source.nextIsWhitespace() && !source.nextIs(BYTESTRING_SQUAREBRACKET_CLOSE) && !source.nextIs(
                        BYTESTRING_COMMA
                    )
                ) {
                    numberFormatError = true
                }
            }

            null -> error("should not happen")
        }

        if (numberFormatError) {
            throw NumberFormatException("unexpected next character: '${source.readUtf8CodePoint().toChar()}'")
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
        skipWhitespaceAndOptionalCommaAndUpdateStack()
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
        skipWhitespaceAndOptionalCommaAndUpdateStack()
        expectValueAndUpdateStack()

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
        skipWhitespaceAndOptionalCommaAndUpdateStack()
        expectValueAndUpdateStack()

        /* opening quote */
        skipSpecific(BYTESTRING_DOUBLEQUOTE)

        val buffer: Buffer? = if (skipString) null else sharedBuffer
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

        val result = buffer?.readUtf8()
        buffer?.clear()

        return result
    }

    /**
     * Returns the next token without consuming it.
     */
    fun peek(): JsonToken {
        skipWhitespaceAndOptionalCommaAndUpdateStack()

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

    private fun expectValueAndUpdateStack() {
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

    private fun skipWhitespaceAndOptionalCommaAndUpdateStack() {
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
