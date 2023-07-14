package com.fab1an.kotlinjsonstream

import okio.BufferedSink

/**
 * Writes a JSON (<a href="http://www.ietf.org/rfc/rfc7159.txt">RFC 7159</a>)
 * encoded value to a stream. This stream includes both literal
 * values (strings, numbers, booleans, and nulls) as well as the begin and
 * end delimiters of objects and arrays.
 *
 * @constructor Creates a new instance that reads a JSON-encoded stream to [sink]. If [prettyPrint] is enabled
 *              the output is formatted with indents, spaces and newlines.
 */
class JsonWriter(private val sink: BufferedSink, val prettyPrint: Boolean = false) {

    private val stack = mutableListOf<OpenToken>()
    private var curIndent = 0

    /**
     * Begins a new array.
     */
    fun beginArray(): JsonWriter {
        expectValue()
        sink.write(BYTESTRING_SQUAREBRACKET_OPEN)
        stack.add(OpenToken.BEGIN_ARRAY)
        if (prettyPrint) {
            curIndent += INDENT
        }

        return this
    }

    /**
     * Begins a new object.
     */
    fun beginObject(): JsonWriter {
        expectValue()
        sink.write(BYTESTRING_CURLYBRACKET_OPEN)
        stack.add(OpenToken.BEGIN_OBJECT)
        if (prettyPrint) {
            curIndent += INDENT
        }

        return this
    }

    /**
     * Closes the underlying [sink].
     */
    fun close() {
        sink.close()
    }

    /**
     * Ends the current array.
     */
    fun endArray(): JsonWriter {
        check(stack.lastOrNull().let { it == OpenToken.BEGIN_ARRAY || it == OpenToken.CONTINUE_ARRAY }) {
            stack.lastOrNull() ?: "empty"
        }
        if (prettyPrint) {
            sink.write(BYTESTRING_NEWLINE)
            curIndent -= INDENT
            writeIndent()
        }
        sink.write(BYTESTRING_SQUAREBRACKET_CLOSE)
        stack.removeLast()

        return this
    }

    /**
     * Ends the current object.
     */
    fun endObject(): JsonWriter {
        check(stack.lastOrNull().let { it == OpenToken.BEGIN_OBJECT || it == OpenToken.CONTINUE_OBJECT }) {
            stack.lastOrNull() ?: "empty"
        }
        if (prettyPrint) {
            sink.write(BYTESTRING_NEWLINE)
            curIndent -= INDENT
            writeIndent()
        }
        sink.write(BYTESTRING_CURLYBRACKET_CLOSE)
        stack.removeLast()

        return this
    }

    /**
     * Begins a new property in the current object, by setting its [name].
     */
    fun name(name: String): JsonWriter {
        check(stack.lastOrNull().let { it == OpenToken.BEGIN_OBJECT || it == OpenToken.CONTINUE_OBJECT }) {
            stack.lastOrNull() ?: "empty"
        }
        if (stack.lastOrNull() == OpenToken.CONTINUE_OBJECT) {
            sink.write(BYTESTRING_COMMA)

        } else if (stack.lastOrNull() == OpenToken.BEGIN_OBJECT) {
            stack.removeLast()
            stack.add(OpenToken.CONTINUE_OBJECT)
        }
        if (prettyPrint) {
            sink.write(BYTESTRING_NEWLINE)
            writeIndent()
        }
        sink.write(BYTESTRING_DOUBLEQUOTE)
        sink.writeUtf8(name)
        sink.write(BYTESTRING_DOUBLEQUOTE)
        sink.write(BYTESTRING_COLON)
        if (prettyPrint) {
            sink.write(BYTESTRING_SPACE)
        }
        stack.add(OpenToken.BEGIN_PROPERTY)
        return this
    }

    /**
     * Writes [value].
     */
    fun value(value: Boolean): JsonWriter {
        expectValue()
        if (value) {
            sink.write(BYTESTRING_TRUE)
        } else {
            sink.write(BYTESTRING_FALSE)
        }

        return this
    }

    /**
     * Writes [value].
     */
    fun value(value: Double): JsonWriter {
        expectValue()
        if (value.rem(1) == 0.0) {
            sink.writeUtf8(value.toInt().toString())

        } else {
            sink.writeUtf8(value.toString())
        }

        return this
    }

    /**
     * Writes [value].
     */
    fun value(value: Long): JsonWriter {
        expectValue()
        sink.writeUtf8(value.toString())

        return this
    }

    /**
     * Writes [value].
     */
    fun value(value: Int): JsonWriter {
        expectValue()
        sink.writeUtf8(value.toString())
        return this
    }

    /**
     * Writes [value].
     */
    fun value(value: String?): JsonWriter {
        expectValue()
        if (value != null) {
            sink.write(BYTESTRING_DOUBLEQUOTE)
            sink.writeUtf8(
                value
                    .replace("\\", """\\""")
                    .replace("\"", """\"""")
                    .replace("/", """\/""")
                    .replace("\b", """\b""")
                    .replace("\u000c", """\f""")
                    .replace("\n", """\n""")
                    .replace("\r", """\r""")
                    .replace("\t", """\t""")
            )
            sink.write(BYTESTRING_DOUBLEQUOTE)
        } else {
            sink.write(BYTESTRING_NULL)
        }
        return this
    }

    /**
     * Writes `null`.
     */
    fun nullValue(): JsonWriter {
        expectValue()

        sink.write(BYTESTRING_NULL)
        return this
    }

    private fun expectValue() {
        check(stack.lastOrNull().let {
            it == OpenToken.BEGIN_ARRAY
                    || it == OpenToken.CONTINUE_ARRAY
                    || it == OpenToken.BEGIN_PROPERTY
                    || it == null
        }) {
            stack.lastOrNull() ?: "empty"
        }

        if (stack.lastOrNull() == OpenToken.BEGIN_PROPERTY) {
            stack.removeLast()

        } else if (stack.lastOrNull() == OpenToken.CONTINUE_ARRAY) {
            sink.write(BYTESTRING_COMMA)
            if (prettyPrint) {
                sink.write(BYTESTRING_NEWLINE)
                writeIndent()
            }

        } else if (stack.lastOrNull() == OpenToken.BEGIN_ARRAY) {
            stack.removeLast()
            stack.add(OpenToken.CONTINUE_ARRAY)
            if (prettyPrint) {
                sink.write(BYTESTRING_NEWLINE)
                writeIndent()
            }
        }
    }

    private enum class OpenToken {
        BEGIN_OBJECT,
        BEGIN_ARRAY,
        BEGIN_PROPERTY,
        CONTINUE_OBJECT,
        CONTINUE_ARRAY
    }

    private fun writeIndent() {
        for (i in 0 until curIndent) {
            sink.write(BYTESTRING_SPACE)
        }
    }

    companion object {
        private const val INDENT = 4
    }
}
