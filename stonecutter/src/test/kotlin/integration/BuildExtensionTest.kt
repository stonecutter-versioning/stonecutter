package integration

import gradle.GradleTest
import gradle.fail
import gradle.should
import io.kotest.core.spec.style.ShouldSpec

class BuildExtensionTest : GradleTest, ShouldSpec({
    context("process parameters") {
        should("allow regular values") { _, build -> build.run() }
        should("validate constant names") { _, build -> build.fail() }
        should("validate swap names") { _, build -> build.fail() }
        should("validate dependency names") { _, build -> build.fail() }
    }
})