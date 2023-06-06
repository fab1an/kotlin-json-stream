package com.fab1an.kotlinjsonstream

import kotlin.test.assertEquals

internal infix fun <T> T.shouldEqual(expected: T) {
    assertEquals(expected, this)
}
