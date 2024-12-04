# Kotlin Json Stream - Kotlin-Multiplatform JSON stream serialization

[![maven central version](https://img.shields.io/maven-central/v/com.fab1an/kotlin-json-stream)](https://mvnrepository.com/artifact/com.fab1an/kotlin-json-stream)
[![semver](https://img.shields.io/:semver-%E2%9C%93-brightgreen.svg)](http://semver.org/)
[![license](https://img.shields.io/github/license/fab1an/kotlin-json-stream)](https://github.com/fab1an/kotlin-json-stream/blob/master/LICENSE)
[![build status](https://github.com/fab1an/kotlin-json-stream/actions/workflows/build-master.yml/badge.svg)](https://github.com/fab1an/kotlin-json-stream/actions/workflows/build-master.yml)
[![OpenSSF ScoreCard](https://img.shields.io/ossf-scorecard/github.com/fab1an/kotlin-json-stream)](https://scorecard.dev/)
[![OpenSSF Best Practices](https://www.bestpractices.dev/projects/8911/badge)](https://www.bestpractices.dev/projects/8911)

## Dokumentation

This library is a kotlin-multiplatform streaming JSON-parser. It is based on OKIO for performance.

### API-Docs
https://fab1an.github.io/kotlin-json-stream/

### Example

```kotlin
fun test() {
    val json = JsonReader("""{"stringProp":"string", "intProp":0}""")

    json.beginObject()
    json.nextName() shouldEqual "stringProp"
    json.nextString() shouldEqual "string"
    json.nextName() shouldEqual "intProp"
    json.nextInt() shouldEqual 0
    json.endObject()
}
  
infix fun <T> T.shouldEqual(expected: T) {
    assertEquals(expected, this)
}
```
