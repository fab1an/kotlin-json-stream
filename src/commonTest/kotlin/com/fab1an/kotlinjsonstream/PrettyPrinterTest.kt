package com.fab1an.kotlinjsonstream

import kotlin.test.Test

class PrettyPrinterTest {

    @Test
    fun prettyPrintArray() {
        val json = """[ "a", "b", 1  ]"""

        prettyPrintJson(json) shouldEqual """
            [
                "a",
                "b",
                1
            ]
        """.trimIndent()
    }

    @Test
    fun prettyPrintSimple() {
        val json = """{ "a": "Str", "b": 1  }"""

        prettyPrintJson(json) shouldEqual """
            {
                "a": "Str",
                "b": 1
            }
        """.trimIndent()
    }

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
        val json =
            // language=json
            """
               {
                    "a1": [], 
                    "a": [[], [], [{"c":[]}]]
                }
            """.trimIndent()

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
