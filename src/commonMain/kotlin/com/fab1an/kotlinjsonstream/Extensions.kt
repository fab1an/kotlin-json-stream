package com.fab1an.kotlinjsonstream

/**
 * Writes a [list] of [E] to this [JsonWriter], using the supplied [writerFn].
 *
 * @param E the type of the objects
 * @param list the list of objects
 * @param writerFn the writer function that is called using each item of the [list] as input
 */
inline fun <E : Any> JsonWriter.value(list: List<E>, writerFn: JsonWriter.(E) -> Unit) {
    beginArray()
    list.forEach { writerFn(it) }
    endArray()
}

/**
 * Reads a [list] of [E] from this [JsonReader]. This function assumes it is called at the start
 * of a JSON-array and reads each element using the supplied [readerFn].
 *
 * @param E the type of the objects
 * @param readerFn the reader function that is called using each item of the array as input
 * @return list of E
 */
inline fun <E : Any> JsonReader.nextList(readerFn: JsonReader.() -> E): List<E> {
    val list = mutableListOf<E>()

    beginArray()
    while (hasNext()) {
        list.add(readerFn())
    }
    endArray()

    return list
}

inline fun <E : Any> JsonWriter.value(set: Set<E>, writerFn: JsonWriter.(E) -> Unit) {
    beginArray()
    set.forEach { writerFn(it) }
    endArray()
}

inline fun <E : Any> JsonReader.nextSet(readerFn: JsonReader.() -> E): Set<E> {
    val set = mutableSetOf<E>()

    beginArray()
    while (hasNext()) {
        set.add(readerFn())
    }
    endArray()

    return set
}

inline fun <E : Any> JsonReader.nextOrNull(readerFn: JsonReader.() -> E): E? {
    return if (peek() == JsonToken.NULL) {
        skipValue()
        null
    } else {
        readerFn()
    }
}
