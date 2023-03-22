package com.fab1an.kotlinjsonstream

import okio.BufferedSink

class JsonWriter(private val sink: BufferedSink, val prettyPrint: Boolean = false) {

    private val stack = mutableListOf<OpenToken>()
    private var curIndent = 0

    fun beginArray(): JsonWriter {
        expectValue()
        sink.writeUtf8("[")
        stack.add(OpenToken.BEGIN_ARRAY)
        if (prettyPrint) {
            curIndent += indent
        }

        return this
    }

    fun beginObject(): JsonWriter {
        expectValue()
        sink.writeUtf8("{")
        stack.add(OpenToken.BEGIN_OBJECT)
        if (prettyPrint) {
            curIndent += indent
        }

        return this
    }

    fun close() {
        sink.close()
    }

    fun endArray(): JsonWriter {
        check(stack.lastOrNull() in setOf(OpenToken.BEGIN_ARRAY, OpenToken.CONTINUE_ARRAY)) {
            stack.lastOrNull() ?: "empty"
        }
        if (prettyPrint) {
            sink.writeUtf8("\n")
            curIndent -= indent
            sink.writeUtf8(" ".repeat(curIndent))
        }
        sink.writeUtf8("]")
        stack.removeLast()

        return this
    }

    fun endObject(): JsonWriter {
        check(stack.lastOrNull() in setOf(OpenToken.BEGIN_OBJECT, OpenToken.CONTINUE_OBJECT)) {
            stack.lastOrNull() ?: "empty"
        }
        if (prettyPrint) {
            sink.writeUtf8("\n")
            curIndent -= indent
            sink.writeUtf8(" ".repeat(curIndent))
        }
        sink.writeUtf8("}")
        stack.removeLast()

        return this
    }

    fun name(name: String): JsonWriter {
        check(stack.lastOrNull() in setOf(OpenToken.BEGIN_OBJECT, OpenToken.CONTINUE_OBJECT)) {
            stack.lastOrNull() ?: "empty"
        }
        if (stack.lastOrNull() == OpenToken.CONTINUE_OBJECT) {
            sink.writeUtf8(",")

        } else if (stack.lastOrNull() == OpenToken.BEGIN_OBJECT) {
            stack.removeLast()
            stack.add(OpenToken.CONTINUE_OBJECT)
        }
        if (prettyPrint) {
            sink.writeUtf8("\n")
            sink.writeUtf8(" ".repeat(curIndent))
        }
        sink.writeUtf8("\"")
        sink.writeUtf8(name)
        sink.writeUtf8("\"")
        sink.writeUtf8(":")
        if (prettyPrint) {
            sink.writeUtf8(" ")
        }
        stack.add(OpenToken.BEGIN_PROPERTY)
        return this
    }

    fun value(value: Boolean): JsonWriter {
        expectValue()
        sink.writeUtf8(value.toString())

        return this
    }

    fun value(value: Double): JsonWriter {
        expectValue()
        if (value.rem(1) == 0.0) {
            sink.writeUtf8(value.toInt().toString())

        } else {
            sink.writeUtf8(value.toString())
        }

        return this
    }

    fun value(value: Long): JsonWriter {
        expectValue()
        sink.writeUtf8(value.toString())

        return this
    }

    fun value(value: Int): JsonWriter {
        expectValue()
        sink.writeUtf8(value.toString())
        return this
    }

    fun value(value: String?): JsonWriter {
        expectValue()
        if (value != null) {
            sink.writeUtf8("\"")
            sink.writeUtf8(value.replace("\"", "\\\""))
            sink.writeUtf8("\"")
        } else {
            sink.writeUtf8("null")
        }
        return this
    }

    fun nullValue(): JsonWriter {
        expectValue()

        sink.writeUtf8("null")
        return this
    }

    private fun expectValue() {
        check(
            stack.lastOrNull() in setOf(
                OpenToken.BEGIN_ARRAY,
                OpenToken.CONTINUE_ARRAY,
                OpenToken.BEGIN_PROPERTY,
                null
            )
        ) {
            stack.lastOrNull() ?: "empty"
        }

        if (stack.lastOrNull() == OpenToken.BEGIN_PROPERTY) {
            stack.removeLast()

        } else if (stack.lastOrNull() == OpenToken.CONTINUE_ARRAY) {
            sink.writeUtf8(",")
            if (prettyPrint) {
                sink.writeUtf8("\n")
                sink.writeUtf8(" ".repeat(curIndent))
            }

        } else if (stack.lastOrNull() == OpenToken.BEGIN_ARRAY) {
            stack.removeLast()
            stack.add(OpenToken.CONTINUE_ARRAY)
            if (prettyPrint) {
                sink.writeUtf8("\n")
                sink.writeUtf8(" ".repeat(curIndent))
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

    companion object {
        private const val indent = 4
    }
}
