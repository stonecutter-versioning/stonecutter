package integration

import gradle.GradleTest
import gradle.minus
import gradle.fail
import gradle.read
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.*
import kotlin.io.path.useLines

@Deprecated("Should be migrated to dedicated tests")
class SyntaxTest : GradleTest, FreeSpec({
    "simple version" - { _, build ->
        build.run(":1:run").output shouldContain "Hello world!"
        build.run(":2:run").output shouldNotContain "Hello world!"
    }

    "unclosed scope" - { _, build ->
        val err = build.fail("run")
        // Check the correct position for the error
        err.buildResult.output shouldContain "Example.java:3:19"
    }

    /**
     * Checks `^/ * /` (without spaces) being incorrectly matched.
     * @see <a href="https://codeberg.org/stonecutter/stonecutter/issues/22">#22</a>
     */
    "nested line scope".config(enabled = false) - { _, build ->
        build.run("stonecutterSwitchTo1")
        build.run(":1:run").output shouldNotContain "Hello world!"
        build.run(":2:run").output shouldNotContain "Hello world!"
        build.run(":3:run").output shouldContain "Hello world!"
    }

    "simple swap" - { _, build ->
        build.run(":1:run").output shouldContain "Hello Tim!"
        build.run(":2:run").output shouldContain "Hello Lace!"
    }

    "swap arguments" - { _, build ->
        build.run(":1:run").output shouldContain "Hello Lace!"
        build.run(":2:run").output shouldContain "Bye Lace!"
    }

    "swap indents" - { directory, build ->
        build.run("stonecutterSwitchTo2")
        directory.resolve("src/main/java/Example.java")
            .useLines { lines -> lines.first { "Hello Lace!" in it } }
            .shouldStartWith(" ".repeat(8))
    }

    "simple constant" - { _, build ->
        build.run(":1:run").output shouldContain "Hello world!"
        build.run(":2:run").output shouldNotContain "Hello world!"
    }

    "simple dependency" - { _, build ->
        build.run(":1:run").output shouldContain "Hello world!"
        build.run(":2:run").output shouldNotContain "Hello world!"
    }

    "if-else chain" - { _, build ->
        for (i in 1..4) build.run(":$i:run").output shouldContain "$i!"
    }

    // TODO: Not critical
    "duplicate else".config(enabled = false) - { _, build ->
        val err = build.fail("run")
        // Check the correct position for the error
        err.buildResult.output shouldContain "Example.java:7:16"
    }

    "no newline" - { directory, build ->
        build.run("stonecutterSwitchTo2")
        build.run("stonecutterSwitchTo1")
        build.run(":1:run").output shouldContain "Hello world!"
        directory read "src/main/java/Example.java" shouldNotContain "<EOF>"
    }

    "included comments" - { directory, build ->
        build.run("stonecutterSwitchTo2")
        directory read "src/main/java/Example.java" shouldContain "/^¹nested¹^/"

        build.run("stonecutterSwitchTo1")
        directory read "src/main/java/Example.java" shouldContain "/^nested^/"
    }

    "nested conditions" - { _, build ->
        build.run("stonecutterSwitchTo1.20.1")
        build.run(":1.20.1:run").output shouldContain "CASE B"
    }

    /**
     * Checks if nested comments are correctly counted for Kotlin files.
     * @see <a href="https://codeberg.org/stonecutter/stonecutter/issues/18">#18</a>
     */
    "nested conditions kt" - { directory, build ->
        build.run("stonecutterSwitchTo1.14.4")
        val file = directory read "src/main/java/PopupScreen.kt"
        val line = file.lines()[24]
        line shouldBe "      /*renderBlurredBackground(/*? if <=1.21.1 {*/partialTick/*?}*/)"
    }

    /**
     * Checks against a weird issue of nested comments copying characters from the comment.
     * @see <a href="https://codeberg.org/stonecutter/stonecutter/issues/19">#19</a>
     */
    "nested conditions err" - { directory, build ->
        build.run("stonecutterSwitchTo1.20.1")
        build.run("stonecutterSwitchTo1.21.6")
        val file = directory read "src/main/java/WaterFogEnvironmentMixin.java"
        val line = file.lines()[28]
        line shouldBe "    /*@SuppressWarnings(\"rawtypes\")*/"
    }

    /**
     * Default `$ swap >>` scopes should first skip the empty region,
     * and then capture everything until the first whitespace.
     * @see <a href="https://codeberg.org/stonecutter/stonecutter/issues/17">#17</a>
     */
    "word scope default" - { _, build ->
        build.run(":2:run").output shouldContain "Bye Lace!"
    }

    /**
     * Custom matchers `$ swap >> 'str'` should match the string,
     * without including it in the scope.
     * @see <a href="https://codeberg.org/stonecutter/stonecutter/issues/17">#17</a>
     */
    "word scope custom" - { _, build ->
        build.run(":2:run").output shouldContain "Bye Tim!"
    }

    /**
     * Custom matchers `$ swap >>+ 'str'` should match the string,
     * **including** it in the scope.
     * @see <a href="https://codeberg.org/stonecutter/stonecutter/issues/17">#17</a>
     */
    "word scope capturing" - { _, build ->
        build.run(":2:run").output shouldContain "Bye Tim!"
    }

    /**
     * Custom matchers that couldn't be satisfied until
     * the scope was closed should report an error.
     * @see <a href="https://codeberg.org/stonecutter/stonecutter/issues/17">#17</a>
     */
    "word scope unmatched" - { _, build ->
        build.fail(":2:run").buildResult.output shouldContain "Failed to find the matching string"
    }

    /**
     * Single-line scopes should exclude the newline.
     * @see <a href="https://codeberg.org/stonecutter/stonecutter/issues/21">#21</a>
     */
    "line handling" - { directory, build ->
        build.run("stonecutterSwitchTo1")
        val file = directory read "src/main/java/Example.java"
        val line = file.lines()[3]
        line shouldBe "        //System.out.println(\"Hello world!\");"
    }

    // TODO: Check fragmented content merging
})