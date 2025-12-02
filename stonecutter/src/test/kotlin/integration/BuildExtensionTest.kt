package integration

import gradle.GradleTest
import gradle.fail
import gradle.read
import gradle.should
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.string.shouldContainOnlyOnce

class BuildExtensionTest : GradleTest, ShouldSpec({
    context("process parameters") {
        should("allow regular values") { _, build -> build.run() }
        should("validate constant names") { _, build -> build.fail() }
        should("validate swap names") { _, build -> build.fail() }
        should("validate dependency names") { _, build -> build.fail() }

        should("allow shared replacement ids") { _, build -> build.run() }
        should("validate replacement ids") { _, build -> build.fail() }
        should("require replacement direction") { _, build -> build.fail() }
        should("register_multiple_replacements") { dir, build ->
            build.run("stonecutterSwitchTo2")
            with(dir read "src/main/java/Example.java") {
                shouldContainOnlyOnce("a.d.c")
                shouldContainOnlyOnce("1.3.4")
            }
        }
    }
})