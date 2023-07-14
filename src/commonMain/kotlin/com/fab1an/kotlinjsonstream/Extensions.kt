package com.fab1an.kotlinjsonstream

/**
 * Writes the [list] of [E] to this [JsonWriter], using the supplied [writerFn].
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
 * Reads a [List] of [E] from this [JsonReader]. This function assumes it is called at the start
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

/**
 * Writes the [set] of [E] to this [JsonWriter], using the supplied [writerFn].
 *
 * @param E the type of the objects
 * @param set the set of objects
 * @param writerFn the writer function that is called using each item of the [list] as input
 */
inline fun <E : Any> JsonWriter.value(set: Set<E>, writerFn: JsonWriter.(E) -> Unit) {
    beginArray()
    set.forEach { writerFn(it) }
    endArray()
}

/**
 * Reads a [Set] of [E] from this [JsonReader]. This function assumes it is called at the start
 * of a JSON-array and reads each element using the supplied [readerFn].
 *
 * @param E the type of the objects
 * @param readerFn the reader function that is called using each item of the array as input
 * @return set of E
 */
inline fun <E : Any> JsonReader.nextSet(readerFn: JsonReader.() -> E): Set<E> {
    val set = mutableSetOf<E>()

    beginArray()
    while (hasNext()) {
        set.add(readerFn())
    }
    endArray()

    return set
}

/**
 * Reads the next value or array item as an [E] from this [JsonReader] using the supplied [readerFn],
 * or null if the item or value is null
 *
 * @param E the type of the object
 * @param readerFn the reader function that is called using each item of the array as input
 * @return E or null
 */
inline fun <E : Any> JsonReader.nextOrNull(readerFn: JsonReader.() -> E): E? {
    return if (peek() == JsonToken.NULL) {
        skipValue()
        null
    } else {
        readerFn()
    }
}
