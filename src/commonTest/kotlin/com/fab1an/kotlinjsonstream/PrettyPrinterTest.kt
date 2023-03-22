package com.fab1an.kotlinjsonstream

import kotlin.test.Test

class PrettyPrinterTest {

    @Test
    fun testPrettyPrint() {
        val json = """{ "a": "Str", "b": [1, 2, 3], "c": [{"c1":1}, {"c2":2}]  }"""

        prettyPrintJson(json) shouldEqual """
            {
                "a": "Str",
                "b": [
                    1,
                    2,
                    3
                ],
                "c": [
                    {
                        "c1": 1
                    },
                    {
                        "c2": 2
                    }
                ]
            }
        """.trimIndent()
    }

    @Test
    fun testNestedAndEmptyArray() {
        val json = """{ "a1": [], "a": [[], [], [{"c":[]}]]}"""

        prettyPrintJson(json) shouldEqual """
            {
                "a1": [
                ],
                "a": [
                    [
                    ],
                    [
                    ],
                    [
                        {
                            "c": [
                            ]
                        }
                    ]
                ]
            }
        """.trimIndent()
    }
}
