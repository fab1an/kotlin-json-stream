package com.fab1an.kotlinjsonstream

import okio.BufferedSource
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import okio.Options

internal val BYTESTRING_ZERO = ByteString.of('0'.code.toByte())
private val BYTESTRING_ONE = ByteString.of('1'.code.toByte())
private val BYTESTRING_TWO = ByteString.of('2'.code.toByte())
private val BYTESTRING_THREE = ByteString.of('3'.code.toByte())
private val BYTESTRING_FOUR = ByteString.of('4'.code.toByte())
private val BYTESTRING_FIVE = ByteString.of('5'.code.toByte())
private val BYTESTRING_SIX = ByteString.of('6'.code.toByte())
private val BYTESTRING_SEVEN = ByteString.of('7'.code.toByte())
private val BYTESTRING_EIGHT = ByteString.of('8'.code.toByte())
private val BYTESTRING_NINE = ByteString.of('9'.code.toByte())
internal val BYTESTRING_HYPHEN = ByteString.of('-'.code.toByte())

internal val BYTESTRING_TAB = ByteString.of('\t'.code.toByte())
internal val BYTESTRING_CARRIAGERETURN = ByteString.of('\r'.code.toByte())
internal val BYTESTRING_NEWLINE = ByteString.of('\n'.code.toByte())
internal val BYTESTRING_SPACE = ByteString.of(' '.code.toByte())

internal val BYTESTRING_BACKSLASH_OR_DOUBLEQUOTE = "\\\"".encodeUtf8()
internal val BYTESTRING_SQUAREBRACKET_OPEN = ByteString.of('['.code.toByte())
internal val BYTESTRING_SQUAREBRACKET_CLOSE = ByteString.of(']'.code.toByte())
internal val BYTESTRING_CURLYBRACKET_OPEN = ByteString.of('{'.code.toByte())
internal val BYTESTRING_CURLYBRACKET_CLOSE = ByteString.of('}'.code.toByte())
internal val BYTESTRING_COLON = ByteString.of(':'.code.toByte())
internal val BYTESTRING_DOUBLEQUOTE = ByteString.of('"'.code.toByte())
internal val BYTESTRING_DOT = ByteString.of('.'.code.toByte())
internal val BYTESTRING_LOWERCASE_E= ByteString.of('e'.code.toByte())
internal val BYTESTRING_UPPERCASE_E= ByteString.of('E'.code.toByte())
internal val BYTESTRING_COMMA = ByteString.of(','.code.toByte())
internal val BYTESTRING_PLUS = ByteString.of('+'.code.toByte())
internal val BYTESTRING_BACKSLASH = ByteString.of('\\'.code.toByte())
internal val BYTESTRING_TRUE = "true".encodeUtf8()
internal val BYTESTRING_FALSE = "false".encodeUtf8()
internal val BYTESTRING_NULL = "null".encodeUtf8()
internal val BYTESTRING_FORWARDSLASH = "/".encodeUtf8()
internal val BYTESTRING_BACKSPACE = "\b".encodeUtf8()
internal val BYTESTRING_FORMFEED = "\u000c".encodeUtf8()

internal val BYTESTRING_ESCAPED_DOUBLEQUOTE = """\"""".encodeUtf8()
internal val BYTESTRING_ESCAPED_BACKSLASH = """\\""".encodeUtf8()
internal val BYTESTRING_ESCAPED_FORWARDSLASH = """\/""".encodeUtf8()
internal val BYTESTRING_ESCAPED_BACKSPACE = """\b""".encodeUtf8()
internal val BYTESTRING_ESCAPED_FORMFEED = """\f""".encodeUtf8()
internal val BYTESTRING_ESCAPED_NEWLINE = """\n""".encodeUtf8()
internal val BYTESTRING_ESCAPED_CARRIAGERETURN = """\r""".encodeUtf8()
internal val BYTESTRING_ESCAPED_TAB = """\t""".encodeUtf8()
internal val BYTESTRING_START_OF_UNICODE_ESCAPE = """\u""".encodeUtf8()

private val booleanOptions = Options.of(
    BYTESTRING_TRUE,
    BYTESTRING_FALSE
)

internal fun BufferedSource.readJsonBoolean(): Boolean {
    return when (select(booleanOptions)) {
        0 -> true
        1 -> false
        else -> error("expected true/false, but got: ${readUtf8CodePoint()}")
    }
}

internal fun BufferedSource.nextIsHyphenOrAsciiDigit(): Boolean {
    return when {
        nextIsAsciiDigit() -> true
        rangeEquals(0, BYTESTRING_HYPHEN) -> true
        else -> false
    }
}

internal fun BufferedSource.nextIsAsciiDigit(): Boolean {
    return when {
        rangeEquals(0, BYTESTRING_ZERO) -> true
        rangeEquals(0, BYTESTRING_ONE) -> true
        rangeEquals(0, BYTESTRING_TWO) -> true
        rangeEquals(0, BYTESTRING_THREE) -> true
        rangeEquals(0, BYTESTRING_FOUR) -> true
        rangeEquals(0, BYTESTRING_FIVE) -> true
        rangeEquals(0, BYTESTRING_SIX) -> true
        rangeEquals(0, BYTESTRING_SEVEN) -> true
        rangeEquals(0, BYTESTRING_EIGHT) -> true
        rangeEquals(0, BYTESTRING_NINE) -> true
        else -> false
    }
}

internal fun BufferedSource.nextIsAsciiDigitExceptZero(): Boolean {
    return when {
        rangeEquals(0, BYTESTRING_ONE) -> true
        rangeEquals(0, BYTESTRING_TWO) -> true
        rangeEquals(0, BYTESTRING_THREE) -> true
        rangeEquals(0, BYTESTRING_FOUR) -> true
        rangeEquals(0, BYTESTRING_FIVE) -> true
        rangeEquals(0, BYTESTRING_SIX) -> true
        rangeEquals(0, BYTESTRING_SEVEN) -> true
        rangeEquals(0, BYTESTRING_EIGHT) -> true
        rangeEquals(0, BYTESTRING_NINE) -> true
        else -> false
    }
}

internal fun BufferedSource.nextIsWhitespace(): Boolean {
    return when {
        rangeEquals(0, BYTESTRING_TAB) -> true
        rangeEquals(0, BYTESTRING_CARRIAGERETURN) -> true
        rangeEquals(0, BYTESTRING_NEWLINE) -> true
        rangeEquals(0, BYTESTRING_SPACE) -> true
        else -> false
    }
}

private val whitespaceOptions = Options.of(
    BYTESTRING_TAB,
    BYTESTRING_CARRIAGERETURN,
    BYTESTRING_NEWLINE,
    BYTESTRING_SPACE
)

internal fun BufferedSource.skipWhitespace() {
    while (select(whitespaceOptions) != -1) {
        //
    }
}


internal fun BufferedSource.nextIs(byteString: ByteString): Boolean {
    return rangeEquals(0, byteString)
}
