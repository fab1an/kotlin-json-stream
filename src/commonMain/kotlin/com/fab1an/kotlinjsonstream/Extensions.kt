package com.fab1an.kotlinjsonstream

inline fun <E : Any> JsonWriter.value(list: List<E>, writerFn: JsonWriter.(E) -> Unit) {
    beginArray()
    list.forEach { writerFn(it) }
    endArray()
}

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
