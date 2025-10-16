package integration

import gradle.GradleTest
import gradle.minus
import gradle.fail
import gradle.read
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.string.*
import kotlin.io.path.useLines

class SyntaxTest : GradleTest, FreeSpec({
    "simple version" - { directory, build ->
        build.run(":1:run").output shouldContain "Hello world!"
        build.run(":2:run").output shouldNotContain "Hello world!"
    }

    "unclosed scope" - { directory, build ->
        val err = build.fail("run")
        // Check the correct position for the error
        err.buildResult.output shouldContain "Example.java:3:19"
    }

    "nested line scope" - { directory, build ->
        build.run(":1:run").output shouldNotContain "Hello world!"
        build.run(":2:run").output shouldNotContain "Hello world!"
        build.run(":3:run").output shouldContain "Hello world!"
    }

    "simple swap" - { directory, build ->
        build.run(":1:run").output shouldContain "Hello Tim!"
        build.run(":2:run").output shouldContain "Hello Lace!"
    }

    "swap arguments" - { directory, build ->
        build.run(":1:run").output shouldContain "Hello Lace!"
        build.run(":2:run").output shouldContain "Bye Lace!"
    }

    "swap indents" - { directory, build ->
        build.run("stonecutterSwitchTo2")
        directory.resolve("src/main/java/Example.java")
            .useLines { lines -> lines.first { "Hello Lace!" in it } }
            .shouldStartWith(" ".repeat(8))
    }

    "simple constant" - { directory, build ->
        build.run(":1:run").output shouldContain "Hello world!"
        build.run(":2:run").output shouldNotContain "Hello world!"
    }

    "simple dependency" - { directory, build ->
        build.run(":1:run").output shouldContain "Hello world!"
        build.run(":2:run").output shouldNotContain "Hello world!"
    }

    "if-else chain" - { directory, build ->
        for (i in 1..4) build.run(":$i:run").output shouldContain "$i!"
    }

    "duplicate else" - { directory, build ->
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

    "nested conditions" - { directory, build ->
        build.run("stonecutterSwitchTo1.20.1")
        build.run(":1.20.1:run").output shouldContain "CASE B"
    }
})