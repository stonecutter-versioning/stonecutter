@file:Suppress("unused", "PublicApiImplicitType")
package integration

import gradle.GradleProjectTest
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class ReplacementTest : AnnotationSpec(), GradleProjectTest {
    @Test fun simple() = build("simple") { directory, build ->
        build.run(":1:run").output shouldContain "Hello World!"
        build.run(":2:run").output shouldContain "!dlroW olleH"
    }

    @Test fun contained() = build("contained") { directory, build ->
        build.run(":2:run").output shouldNotContain "prefix.a.suffix.b.b"
    }

    @Test fun named() = build("named") { directory, build ->
        with(build.run(":1:run").output) {
            shouldContain("Primary Hello World!")
            shouldContain("Secondary Hello World!")
        }

        with(build.run(":2:run").output) {
            shouldContain("Primary Hello World!")
            shouldContain("Secondary !dlroW olleH")
        }
    }
}