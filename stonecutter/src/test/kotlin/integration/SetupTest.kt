@file:Suppress("unused", "PublicApiImplicitType")

package integration

import gradle.GradleProjectTest
import gradle.shouldFailBuild
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.paths.shouldContainFile
import io.kotest.matchers.paths.shouldNotContainFile
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldContainOnlyOnce
import org.gradle.testkit.runner.TaskOutcome
import util.shouldReturn

class SetupTest : AnnotationSpec(), GradleProjectTest {
    @Test fun `kotlin controller init`() = build("builds/init") { directory, build ->
        build.run()
        directory shouldContainFile "stonecutter.gradle.kts"
    }

    @Test fun `groovy top controller init`() = build("builds/init") { directory, build ->
        build.run("-Ptop-groovy-controller=true")
        directory shouldContainFile "stonecutter.gradle"
    }

    @Test fun `groovy tree controller init`() = build("builds/init") { directory, build ->
        build.run("-Ptree-groovy-controller=true")
        directory shouldContainFile "stonecutter.gradle"
    }

    @Test fun `implicit controller init`() = build("builds/init") { directory, build ->
        directory.resolve("stonecutter.gradle") write """
            plugins {
                id "dev.kikugie.stonecutter"
            }
            
            stonecutter.active null
        """.trimIndent()

        build.run()
        directory shouldNotContainFile "stonecutter.gradle.kts"
    }

    @Test fun `normal setup`() = build("builds/normal") { directory, build ->
        with(build.run("printCurrentProject")) {
            task(":1.20.1:printCurrentProject") shouldReturn TaskOutcome.SUCCESS
            task(":1.21.1:printCurrentProject") shouldReturn TaskOutcome.SUCCESS
            task(":snapshot:printCurrentProject") shouldReturn TaskOutcome.SUCCESS
        }
    }

    @Test fun `inconsistent version`() = build("builds/inconsistent") { directory, build ->
        val exception = shouldFailBuild { build.run() }
        exception.message shouldContain "Project 'snapshot' is registered with a different version"
    }

    @Ignore // FIXME: Currently does the same as "inconsistent version"
    @Test fun `json override`() = build("builds/override_json") { directory, build ->
        build.run("printCurrentProject").output shouldContainOnlyOnce "1.21.9"
    }
}