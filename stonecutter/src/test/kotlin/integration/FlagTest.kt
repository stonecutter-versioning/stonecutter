@file:Suppress( "unused", "PublicApiImplicitType", "RemoveRedundantBackticks")

package integration

import dev.kikugie.stonecutter.controller.flag.StonecutterFlag
import gradle.GradleProjectTest
import gradle.shouldFailBuild
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

private fun <T : Any> flag(flag: StonecutterFlag<T>, value: T) = "-Pdev.kikugie.stonecutter.${flag.key}=$value"

class FlagTest : AnnotationSpec(), GradleProjectTest {
    @Test fun `hard_mode`() = build("hard_mode") { directory, build ->
        val header = "NOTICE: Limited Groovy DSL support for Stonecutter"

        // Should print warning
        build.run().output shouldContain header

        // Should omit warning
        build.run("-Pdev.kikugie.stonecutter.hard_mode=true").output shouldNotContain header
    }

    @Test fun `auto_apply_plugin`() = build("auto_apply_plugin") { directory, build ->
        // Should apply plugin
        build.run()

        // Should fail due to unresolved reference
        shouldFailBuild {
            build.run(flag(StonecutterFlag.APPLY_PLUGIN_TO_NODES, false))
        }
    }

    @Ignore // FIXME: Setting the system property doesn't work
    @Test fun `generate_sources_on_sync`() = build("generate_sources_on_sync") { directory, build ->
        build.run("-Didea.sync.active=true")
            .output shouldContain "stonecutterIdea"

        build.run("-Didea.sync.active=true", flag(StonecutterFlag.GENERATE_SOURCES_ON_SYNC, false))
            .output shouldNotContain "stonecutterIdea"

    }

    @Test fun `implicit_receiver`() = build("implicit_receiver") { directory, build ->
        build.run(flag(StonecutterFlag.IMPLICIT_RECEIVER, "minceraft"))

        shouldFailBuild {
            build.run("stonecutterGenerate")
        }
    }
}