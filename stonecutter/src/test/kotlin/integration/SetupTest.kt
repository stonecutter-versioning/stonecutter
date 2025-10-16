package integration

import gradle.GradleTest
import gradle.build
import gradle.write
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.paths.shouldContainFile
import io.kotest.matchers.paths.shouldNotContainFile
import org.gradle.testkit.runner.TaskOutcome
import util.shouldReturn

class SetupTest : GradleTest, FreeSpec({
    "kotlin controller init" - {
        build("init") { directory, build ->
            build.run()
            directory shouldContainFile "stonecutter.gradle.kts"
        }
    }

    "groovy top controller init" - {
        build("init") { directory, build ->
            build.run("-Ptop-groovy-controller=true")
            directory shouldContainFile "stonecutter.gradle"
        }
    }

    "groovy tree controller init" - {
        build("init") { directory, build ->
            build.run("-Ptree-groovy-controller=true")
            directory shouldContainFile "stonecutter.gradle"
        }
    }

    "implicit controller init" - {
        build("init") { directory, build ->
            directory.resolve("stonecutter.gradle") write """
                plugins {
                    id "dev.kikugie.stonecutter"
                }
                
                stonecutter.active null
            """.trimIndent()

            build.run()
            directory shouldNotContainFile "stonecutter.gradle.kts"
        }
    }

    "normal setup" - {
        build("normal") { directory, build ->
            with(build.run("printCurrentProject")) {
                task(":1.20.1:printCurrentProject") shouldReturn TaskOutcome.SUCCESS
                task(":1.21.1:printCurrentProject") shouldReturn TaskOutcome.SUCCESS
                task(":snapshot:printCurrentProject") shouldReturn TaskOutcome.SUCCESS
            }
        }
    }
})