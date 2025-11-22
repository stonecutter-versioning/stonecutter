package integration

import gradle.GradleTest
import gradle.minus
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class ReplacementTest : GradleTest, FreeSpec({
    "simple" - { _, build ->
        build.run(":1:run").output shouldContain "Hello World!"
        build.run(":2:run").output shouldContain "!dlroW olleH"
    }

    "repeated" - { _, build ->
        with(build.run(":1:run").output) {
            shouldContain("net.minecraft.resources.ResourceLocation")
            shouldContain("net/minecraft/resources/ResourceLocation")
        }
        with(build.run(":2:run").output) {
            shouldContain("net.minecraft.resources.Identifier")
            shouldContain("net/minecraft/resources/Identifier")
        }
    }

    "contained" - { _, build ->
        build.run(":2:run").output shouldNotContain "prefix.a.suffix.b.b"
    }

    "named" - { _, build ->
        with(build.run(":1:run").output) {
            shouldContain("Primary Hello World!")
            shouldContain("Secondary Hello World!")
        }

        with(build.run(":2:run").output) {
            shouldContain("Primary Hello World!")
            shouldContain("Secondary !dlroW olleH")
        }
    }
})