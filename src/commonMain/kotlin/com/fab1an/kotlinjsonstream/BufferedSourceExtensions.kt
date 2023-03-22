package com.fab1an.kotlinjsonstream

import okio.BufferedSource
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8

internal fun BufferedSource.nextIsAsciiDigit(): Boolean {
    return when {
        rangeEquals(0, ByteString.of('0'.code.toByte())) -> true
        rangeEquals(0, ByteString.of('1'.code.toByte())) -> true
        rangeEquals(0, ByteString.of('2'.code.toByte())) -> true
        rangeEquals(0, ByteString.of('3'.code.toByte())) -> true
        rangeEquals(0, ByteString.of('4'.code.toByte())) -> true
        rangeEquals(0, ByteString.of('5'.code.toByte())) -> true
        rangeEquals(0, ByteString.of('6'.code.toByte())) -> true
        rangeEquals(0, ByteString.of('7'.code.toByte())) -> true
        rangeEquals(0, ByteString.of('8'.code.toByte())) -> true
        rangeEquals(0, ByteString.of('9'.code.toByte())) -> true
        rangeEquals(0, ByteString.of('-'.code.toByte())) -> true
        else -> false
    }
}

internal fun BufferedSource.nextIsJsonWhiteSpace(): Boolean {
    return when {
        rangeEquals(0, ByteString.of('\t'.code.toByte())) -> true
        rangeEquals(0, ByteString.of('\n'.code.toByte())) -> true
        rangeEquals(0, ByteString.of('\r'.code.toByte())) -> true
        rangeEquals(0, ByteString.of(' '.code.toByte())) -> true
        else -> false
    }
}

internal fun BufferedSource.nextIs(char: Char): Boolean {
    return nextIs(char.toString())
}

internal fun BufferedSource.nextIs(string: String): Boolean {
    return rangeEquals(0, string.encodeUtf8())
}
