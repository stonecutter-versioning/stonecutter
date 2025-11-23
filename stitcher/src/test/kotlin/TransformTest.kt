import dev.kikugie.semver.data.SemanticVersion
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import util.process

class TransformTest : FreeSpec({
    "replacements" - {
        "simple string" {
            val content = """
                Hello World!
            """.trimIndent()
            val expected = """
                Hello Lace!
            """.trimIndent()
            process(content) {
                replacements.string("World", "Lace")
            } shouldBe expected
        }

        "contained string" {
            val content = """
                a.b.c
            """.trimIndent()
            val expected = """
                a.b.c
            """.trimIndent()
            process(content) {
                replacements.string("b", "a.b.c")
            } shouldBe expected
        }

        "longest string" {
            val content = """
                a.b.c
            """.trimIndent()
            val expected = """
                e.c
            """.trimIndent()
            process(content) {
                replacements.string("a", "d")
                replacements.string("a.b", "e")
            } shouldBe expected
        }

        "active identifier" {
            val content = """
                //~ example
                a.b.c
            """.trimIndent()
            val expected = """
                //~ example
                a.d.c
            """.trimIndent()
            process(content) {
                replacements.string("b", "d", "example")
            } shouldBe expected
        }

        "inactive identifier" {
            val content = """
                a.b.c
            """.trimIndent()
            val expected = """
                a.b.c
            """.trimIndent()
            process(content) {
                replacements.string("b", "d", "example")
            } shouldBe expected
        }

        // Matches inside comments shouldn't be replaced
        "commented values" {
            val content = """
                a.b.c
                // a.b.c
            """.trimIndent()
            val expected = """
                a.d.c
                // a.b.c
            """.trimIndent()
            process(content) {
                replacements.string("b", "d")
            } shouldBe expected
        }

        // Matches in what is about to be processed shouldn't be replaced either
        "commented out values" {
            val content = """
                //? if it
                a.b.c
            """.trimIndent()
            val expected = """
                //? if it
                //a.b.c
            """.trimIndent()
            process(content) {
                constants["it"] = false
                replacements.string("b", "d")
            } shouldBe expected
        }

        // Matches in code that was uncommented should be replaced
        "uncommented values" {
            val content = """
                //? if it
                //a.b.c
            """.trimIndent()
            val expected = """
                //? if it
                a.d.c
            """.trimIndent()
            process(content) {
                constants["it"] = true
                replacements.string("b", "d")
            } shouldBe expected
        }
    }

    "conditions" - {
        "disable segment" {
            val content = """
                //? if false
                ...
            """.trimIndent()
            val expected = """
                //? if false
                //...
            """.trimIndent()
            process(content) {
                constants["true"] = true
                constants["false"] = false
            } shouldBe expected
        }

        "enable segment" {
            val content = """
                //? if true
                //...
            """.trimIndent()
            val expected = """
                //? if true
                ...
            """.trimIndent()
            process(content) {
                constants["true"] = true
                constants["false"] = false
            } shouldBe expected
        }

        "unary negation" {
            val content = """
                //? if !true
                ...
            """.trimIndent()
            val expected = """
                //? if !true
                //...
            """.trimIndent()
            process(content) {
                constants["true"] = true
                constants["false"] = false
            } shouldBe expected
        }

        "boolean or" {
            val content = """
                //? if false || true
                //...
            """.trimIndent()
            val expected = """
                //? if false || true
                ...
            """.trimIndent()
            process(content) {
                constants["true"] = true
                constants["false"] = false
            } shouldBe expected
        }

        "boolean and" {
            val content = """
                //? if false && true
                ...
            """.trimIndent()
            val expected = """
                //? if false && true
                //...
            """.trimIndent()
            process(content) {
                constants["true"] = true
                constants["false"] = false
            } shouldBe expected
        }

        "single predicate" {
            val content = """
                //? if >1
                ...
            """.trimIndent()
            val expected = """
                //? if >1
                //...
            """.trimIndent()
            process(content) {
                dependencies[""] = SemanticVersion(intArrayOf(1))
            } shouldBe expected
        }

        "multiple predicates" {
            val content = """
                //? if >1 <3
                //...
            """.trimIndent()
            val expected = """
                //? if >1 <3
                //...
            """.trimIndent()
            process(content) {
                dependencies[""] = SemanticVersion(intArrayOf(3))
            } shouldBe expected
        }

        "named predicates" {
            val content = """
                //? if id: >1 <3
                //...
            """.trimIndent()
            val expected = """
                //? if id: >1 <3
                //...
            """.trimIndent()
            process(content) {
                dependencies["id"] = SemanticVersion(intArrayOf(3))
            } shouldBe expected
        }

        "predicate operator precedence" {
            val content = """
                //? if !=1 <3
                //...
            """.trimIndent()
            val expected = """
                //? if !=1 <3
                ...
            """.trimIndent()
            process(content) {
                dependencies[""] = SemanticVersion(intArrayOf(2))
            } shouldBe expected
        }
    }

    "deep nesting" {
        val content = """
                /*? if false {*/
                    ...
                    /*? if false {*/
                        ...
                        /*? if false {*/
                            ...
                            /*? if false {*/
                                ...
                                /*? if false {*/
                                    ...
                                    /*? if false {*/
                                        ...
                                        /*? if false {*/
                                            ...
                                            /*? if false {*/
                                                ...
                                                /*? if false {*/
                                                    ...
                                                    /*? if false {*/
                                                        ...
                                                        /*? if false {*/
                                                            ...
                                                        /*?}*/
                                                    /*?}*/
                                                /*?}*/
                                            /*?}*/
                                        /*?}*/
                                    /*?}*/
                                /*?}*/
                            /*?}*/
                        /*?}*/
                    /*?}*/
                /*?}*/
            """.trimIndent()
        val expected = """
                /*? if false {*/
                    /*...
                    /^? if false {^/
                        /^...
                        /^¹? if false {¹^/
                            /^¹...
                            /^²? if false {²^/
                                /^²...
                                /^³? if false {³^/
                                    /^³...
                                    /^⁴? if false {⁴^/
                                        /^⁴...
                                        /^⁵? if false {⁵^/
                                            /^⁵...
                                            /^⁶? if false {⁶^/
                                                /^⁶...
                                                /^⁷? if false {⁷^/
                                                    /^⁷...
                                                    /^⁸? if false {⁸^/
                                                        /^⁸...
                                                        /^⁹? if false {⁹^/
                                                            /^⁹...
                                                        ⁹^//^⁹?}⁹^/
                                                    ⁸^//^⁸?}⁸^/
                                                ⁷^//^⁷?}⁷^/
                                            ⁶^//^⁶?}⁶^/
                                        ⁵^//^⁵?}⁵^/
                                    ⁴^//^⁴?}⁴^/
                                ³^//^³?}³^/
                            ²^//^²?}²^/
                        ¹^//^¹?}¹^/
                    ^//^?}^/
                *//*?}*/
            """.trimIndent()
        process(content) {
            constants["false"] = false
        } shouldBe expected
    }
})