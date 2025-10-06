@file:Suppress("unused", "PublicApiImplicitType")
package integration

import gradle.GradleProjectTest
import gradle.shouldFailBuild
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import kotlin.io.path.useLines

class SyntaxTest : AnnotationSpec(), GradleProjectTest {
    @Test fun `simple version`() = build("simple_version") { directory, build ->
        build.run(":1:run").output shouldContain "Hello world!"
        build.run(":2:run").output shouldNotContain "Hello world!"
    }

    @Test fun `unclosed scope`() = build("unclosed_scope") { directory, build ->
        val err = shouldFailBuild {
            build.run("run")
        }

        // Check the correct position for the error
        err.buildResult.output shouldContain "Example.java:3:19"
    }

    @Test fun `nested line scope`() = build("nested_line_scope") { directory, build ->
        build.run(":1:run").output shouldNotContain "Hello world!"
        build.run(":2:run").output shouldNotContain "Hello world!"
        build.run(":3:run").output shouldContain "Hello world!"
    }

    @Test fun `simple swap`() = build("simple_swap") { directory, build ->
        build.run(":1:run").output shouldContain "Hello Tim!"
        build.run(":2:run").output shouldContain "Hello Lace!"
    }

    @Test fun `swap arguments`() = build("swap_arguments") { directory, build ->
        build.run(":1:run").output shouldContain "Hello Lace!"
        build.run(":2:run").output shouldContain "Bye Lace!"
    }

    @Test fun `swap indents`() = build("swap_indents") { directory, build ->
        build.run("stonecutterSwitchTo2")
        directory.resolve("src/main/java/Example.java")
            .useLines { lines -> lines.first { "Hello Lace!" in it } }
            .shouldStartWith(" ".repeat(8))
    }
}