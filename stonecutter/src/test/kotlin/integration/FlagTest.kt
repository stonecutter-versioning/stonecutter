package integration

import dev.kikugie.stonecutter.controller.flag.StonecutterFlag
import gradle.GradleTest
import gradle.minus
import gradle.shouldFailBuild
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

private fun <T : Any> flag(flag: StonecutterFlag<T>, value: T) = "-Pdev.kikugie.stonecutter.${flag.key}=$value"

class FlagTest : GradleTest, FreeSpec({
    "hard mode" - { directory, build ->
        val header = "NOTICE: Limited Groovy DSL support for Stonecutter"

        // Should print warning
        build.run().output shouldContain header

        // Should omit warning
        build.run("-Pdev.kikugie.stonecutter.hard_mode=true").output shouldNotContain header
    }

    "auto apply plugin" - { directory, build ->
        // Should apply plugin
        build.run()

        // Should fail due to unresolved reference
        shouldFailBuild {
            build.run(flag(StonecutterFlag.APPLY_PLUGIN_TO_NODES, false))
        }
    }

    "implicit receiver" - { directory, build ->
        build.run(flag(StonecutterFlag.IMPLICIT_RECEIVER, "minceraft"))

        shouldFailBuild {
            build.run("stonecutterGenerate")
        }
    }

    // FIXME: Setting the system property doesn't work
    "generate sources on sync".config(enabled = false) - { directory, build ->
        build.run("-Didea.sync.active=true")
            .output shouldContain "stonecutterIdea"

        build.run("-Didea.sync.active=true", flag(StonecutterFlag.GENERATE_SOURCES_ON_SYNC, false))
            .output shouldNotContain "stonecutterIdea"
    }
})