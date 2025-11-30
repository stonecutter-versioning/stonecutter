package integration

import gradle.GradleTest
import gradle.should
import io.kotest.core.spec.style.ShouldSpec

class GroovyCompatibilityTest : GradleTest, ShouldSpec({
    context("settings") {
        should("accept combined setup") { _, build -> build.run("stonecutterGenerate") }
        should("use implicit shared") { _, build -> build.run("stonecutterGenerate") }
        should("use explicit shared") { _, build -> build.run("stonecutterGenerate") }
        should("declare branch versions") { _, build -> build.run(":subproject:3:stonecutterGenerate") }
    }
})