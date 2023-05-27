package com.fab1an.kotlinjsonstream

import okio.BufferedSource
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8

private val BYTESTRING_ZERO = ByteString.of('0'.code.toByte())
private val BYTESTRING_ONE = ByteString.of('1'.code.toByte())
private val BYTESTRING_TWO = ByteString.of('2'.code.toByte())
private val BYTESTRING_THREE = ByteString.of('3'.code.toByte())
private val BYTESTRING_FOUR = ByteString.of('4'.code.toByte())
private val BYTESTRING_FIVE = ByteString.of('5'.code.toByte())
private val BYTESTRING_SIX = ByteString.of('6'.code.toByte())
private val BYTESTRING_SEVEN = ByteString.of('7'.code.toByte())
private val BYTESTRING_EIGHT = ByteString.of('8'.code.toByte())
private val BYTESTRING_NINE = ByteString.of('9'.code.toByte())
private val BYTESTRING_HYPHEN = ByteString.of('-'.code.toByte())

private val BYTESTRING_TAB = ByteString.of('\t'.code.toByte())
private val BYTESTRING_CARRIAGE_RETURN = ByteString.of('\r'.code.toByte())
private val BYTESTRING_NEWLINE = ByteString.of('\n'.code.toByte())
private val BYTESTRING_SPACE = ByteString.of(' '.code.toByte())

internal val BYTESTRING_SQUAREBRACKET_OPEN = ByteString.of('['.code.toByte())
internal val BYTESTRING_SQUAREBRACKET_CLOSE = ByteString.of(']'.code.toByte())
internal val BYTESTRING_CURLYBRACKET_OPEN = ByteString.of('{'.code.toByte())
internal val BYTESTRING_CURLYBRACKET_CLOSE = ByteString.of('}'.code.toByte())
internal val BYTESTRING_COLON = ByteString.of(':'.code.toByte())
internal val BYTESTRING_DOUBLEQUOTE = ByteString.of('"'.code.toByte())
internal val BYTESTRING_DOT = ByteString.of('.'.code.toByte())
internal val BYTESTRING_COMMA = ByteString.of(','.code.toByte())
internal val BYTESTRING_TRUE = "true".encodeUtf8()
internal val BYTESTRING_FALSE = "false".encodeUtf8()
internal val BYTESTRING_NULL = "null".encodeUtf8()
internal val BYTESTRING_ESCAPED_DOUBLE_DASH = """\"""".encodeUtf8()
internal val BYTESTRING_ESCAPED_FORWARD_SLASH = """\\""".encodeUtf8()

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
        rangeEquals(0, BYTESTRING_HYPHEN) -> true
        else -> false
    }
}

internal fun BufferedSource.nextIsJsonWhiteSpace(): Boolean {
    return when {
        rangeEquals(0, BYTESTRING_TAB) -> true
        rangeEquals(0, BYTESTRING_NEWLINE) -> true
        rangeEquals(0, BYTESTRING_CARRIAGE_RETURN) -> true
        rangeEquals(0, BYTESTRING_SPACE) -> true
        else -> false
    }
}

internal fun BufferedSource.nextIs(byteString: ByteString): Boolean {
    return rangeEquals(0, byteString)
}
