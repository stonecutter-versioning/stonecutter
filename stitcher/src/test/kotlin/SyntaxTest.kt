import io.kotest.assertions.shouldFail
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.intellij.lang.annotations.Language
import util.process

class SyntaxTest : FreeSpec({
    "basic scopes" - {
        "disable closed" {
            @Language("JAVA") val content = """
                //? if false {
                int one = 1;
                int two = 2;
                //?}
            """.trimIndent()
            @Language("JAVA") val expected = """
                //? if false {
                /*int one = 1;
                int two = 2;
                *///?}
            """.trimIndent()
            process(content) shouldBe expected
        }

        "enable closed" {
            @Language("JAVA") val content = """
                //? if true {
                //int one = 1;
                //int two = 2;
                //?}
            """.trimIndent()
            @Language("JAVA") val expected = """
                //? if true {
                int one = 1;
                int two = 2;
                //?}
            """.trimIndent()
            process(content) shouldBe expected
        }

        "disable line" {
            @Language("JAVA") val content = """
                //? if false
                int one = 1;
                int two = 2;
            """.trimIndent()
            @Language("JAVA") val expected = """
                //? if false
                //int one = 1;
                int two = 2;
            """.trimIndent()
            process(content) shouldBe expected
        }

        "enable line" {
            @Language("JAVA") val content = """
                //? if true
                //int one = 1;
                int two = 2;
            """.trimIndent()
            @Language("JAVA") val expected = """
                //? if true
                int one = 1;
                int two = 2;
            """.trimIndent()
            process(content) shouldBe expected
        }

        "disable word" {
            @Language("JAVA") val content = """
                //? if false >>
                int one = 1;
                int two = 2;
            """.trimIndent()
            @Language("JAVA") val expected = """
                //? if false >>
                /*int*/ one = 1;
                int two = 2;
            """.trimIndent()
            process(content) shouldBe expected
        }

        "enable word" {
            @Language("JAVA") val content = """
                //? if true >>
                /*int*/ one = 1;
                int two = 2;
            """.trimIndent()
            @Language("JAVA") val expected = """
                //? if true >>
                int one = 1;
                int two = 2;
            """.trimIndent()
            process(content) shouldBe expected
        }
    }

    "extended word" - {
        "disable non-capturing" {
            @Language("JAVA") val content = """
                //? if false >> '='
                int one = 1;
                int two = 2;
            """.trimIndent()
            @Language("JAVA") val expected = """
                //? if false >> '='
                /*int one */= 1;
                int two = 2;
            """.trimIndent()
            process(content) shouldBe expected
        }

        "enable non-capturing" {
            @Language("JAVA") val content = """
                //? if true >> '='
                /*int one */= 1;
                int two = 2;
            """.trimIndent()
            @Language("JAVA") val expected = """
                //? if true >> '='
                int one = 1;
                int two = 2;
            """.trimIndent()
            process(content) shouldBe expected
        }

        "disable capturing" {
            @Language("JAVA") val content = """
                //? if false >>+ '='
                int one = 1;
                int two = 2;
            """.trimIndent()
            @Language("JAVA") val expected = """
                //? if false >>+ '='
                /*int one =*/ 1;
                int two = 2;
            """.trimIndent()
            process(content) shouldBe expected
        }

        "enable capturing" {
            @Language("JAVA") val content = """
                //? if true >>+ '='
                /*int one =*/ 1;
                int two = 2;
            """.trimIndent()
            @Language("JAVA") val expected = """
                //? if true >>+ '='
                int one = 1;
                int two = 2;
            """.trimIndent()
            process(content) shouldBe expected
        }

        "fail for not found" {
            @Language("JAVA") val content = """
                //? if false >> '+'
                int one = 1;
                int two = 2;
            """.trimIndent()
            shouldFail { process(content) }
        }

        "search comments" {
            @Language("JAVA") val content = """
                //? if true >>+ '2;'
                //int one = 1;
                //int two = 2;
                // unused
            """.trimIndent()
            @Language("JAVA") val expected = """
                //? if true >>+ '2;'
                int one = 1;
                int two = 2;
                // unused
            """.trimIndent()
            process(content) shouldBe expected
        }

        "split comment" {
            @Language("JAVA") val content = """
                //? if true >>+ 'two ='
                //int one = 1;
                //int two = 2;
                // unused
            """.trimIndent()
            @Language("JAVA") val expected = """
                //? if true >>+ 'two ='
                int one = 1;
                int two = /*2;*/
                // unused
            """.trimIndent()
            process(content) shouldBe expected
        }
    }

    "extended line" - {
        "search comments" {
            @Language("JAVA") val content = """
                //? if true
                /*    */
                //
                //int one = 1;
                //int two = 2;
            """.trimIndent()
            @Language("JAVA") val expected = """
                //? if true
                    
                
                int one = 1;
                //int two = 2;
            """.trimIndent()
            process(content) shouldBe expected
        }

        "split comment" {
            @Language("JAVA") val content = """
                //? if true
                /*
                
                int one = 1;
                int two = 2;
                *///unused
            """.trimIndent()
            @Language("JAVA") val expected = """
                //? if true
                
                
                int one = 1;
                /*int two = 2;
                *///unused
            """.trimIndent()
            process(content) shouldBe expected
        }

        "excluded line break" {
            @Language("JAVA") val content = """
                //? if true
                /*int one = 1;*/
                //unused
            """.trimIndent()
            @Language("JAVA") val expected = """
                //? if true
                int one = 1;
                //unused
            """.trimIndent()
            process(content) shouldBe expected
        }
    }

    // TODO: https://codeberg.org/stonecutter/stonecutter/issues/23
    "read-only comments".config(enabled = false) - {
        "html javadoc" {
            @Language("JAVA") val content = """
                //? if false {
                /**
                 * Example documentation.
                 */
                public void example();
                //?}
            """.trimIndent()
            @Language("JAVA") val expected = """
                //? if false {
                /**
                 * Example documentation.
                 */
                /*public void example();
                *///?}
            """.trimIndent()
            process(content) shouldBe expected
        }

        "markdown javadoc" {
            @Language("JAVA") val content = """
                //? if false {
                /// Example documentation.
                public void example();
                //?}
            """.trimIndent()
            @Language("JAVA") val expected = """
                //? if false {
                /// Example documentation.
                /*public void example();
                *///?}
            """.trimIndent()
            process(content) shouldBe expected
        }

        "scope extension" {
            @Language("JAVA") val content = """
                //? if false
                /// Example documentation.
                public void example();
            """.trimIndent()
            @Language("JAVA") val expected = """
                //? if false
                /// Example documentation.
                /*public void example();*/
            """.trimIndent()
            process(content) shouldBe expected
        }

        "long scopes" {
            @Language("JAVA") val content = """
                //? if false {
                /// Example documentation #1.
                public void example1();
                
                /// Example documentation #2.
                public void example2();
                //?}
            """.trimIndent()
            @Language("JAVA") val expected = """
                //? if false {
                /// Example documentation #1.
                /*public void example1();
                
                *//// Example documentation #2.
                /*public void example2();
                *///?}
            """.trimIndent()
            process(content) shouldBe expected
        }
    }
})