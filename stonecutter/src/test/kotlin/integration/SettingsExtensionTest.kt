package integration

import gradle.GradleTest
import gradle.fail
import gradle.read
import gradle.should
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.paths.shouldContainFile
import io.kotest.matchers.paths.shouldNotContainFile
import io.kotest.matchers.should
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlin.io.path.exists

class SettingsExtensionTest : GradleTest, ShouldSpec({
    should("create extension alias") { _, build -> build.run() }
    should("fail with disabled alias") { _, build -> build.fail() }

    context("script resolution") {
        should("create subproject directories") { dir, build ->
            build.run(); dir.resolve("versions") should { it.exists() }
        }

        should("generate kts scripts by default") { dir, build ->
            build.run(":1:hello").output shouldContain "1!"
            dir shouldContainFile "stonecutter.gradle.kts"
        }

        should("explicitly use groovy scripts") { dir, build ->
            build.run(":1:hello").output shouldContain "1!"
            dir shouldContainFile "stonecutter.gradle"
        }

        should("recognize existing groovy scripts") { dir, build ->
            build.run()
            dir shouldNotContainFile "stonecutter.gradle.kts"
        }

        should("print groovy compatibility warning") { _, build ->
            build.run().output shouldContain "NOTICE:"
        }

        should("mute compat warning with property") { _, build ->
            build.run().output shouldNotContain "NOTICE:"
        }
    }

    context("tree builder") {
        should("use existing shared action") { dir, build ->
            build.run(); dir.resolve("versions") should { it.exists() }
        }

        should("apply shared explicitly") { dir, build ->
            build.run(); dir.resolve("versions") should { it.exists() }
        }

        should("not allow inherit call in root") { _, build ->
            build.fail()
        }

        should("not allow inherit call in default branch") { _, build ->
            build.fail()
        }

        should("implicitly assign vcs version") { dir, build ->
            build.run()
            dir read "stonecutter.gradle.kts" shouldContain("\"3\"")
        }

        should("overwrite vcs version") { dir, build ->
            build.run()
            dir read "stonecutter.gradle.kts" shouldContain("\"1\"")
        }

        should("validate branch names") { _, build ->
            build.fail()
        }

        should("validate version consistency") { _, build ->
            build.fail()
        }

        should("have no vers function") { _, build ->
            build.fail()
        }
    }

    context("tree deserialization") {
        should("accept json files") { _, build -> build.run() }
        should("accept json5 files") { _, build -> build.run() }
        should("reject non-json files") { _, build -> build.fail() }
        should("define project branches") { _, build -> build.run() }
        should("define inverted branches") { _, build -> build.run() }
        should("define extended nodes") { _, build -> build.run() }
    }
})