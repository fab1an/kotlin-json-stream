package com.fab1an.kotlinjsonstream

import okio.Buffer

fun JsonReader(jsonStr: String): JsonReader {
    return JsonReader(Buffer().apply { write(jsonStr.encodeToByteArray()) })
}
