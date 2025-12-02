package integration

import gradle.GradleTest
import gradle.fail
import gradle.read
import gradle.should
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldContainOnlyOnce

class ControllerExtensionTest : GradleTest, ShouldSpec({
    should("create extension alias") { _, build -> build.run() }
    should("fail with disabled alias") { _, build -> build.fail() }
    should("switch active with alias") { dir, build ->
        build.run("stonecutterSwitchTo2")
        dir read "stonecutter.gradle.kts" shouldContain "sc active \"2\""
    }

    context("plugin initialization") {
        should("fail without active call") { _, build -> build.fail() }
        should("warn with base plugin") { _, build -> build.run().output shouldContain "Stonecutter branch root" }
        should("validate active parameter") { _, build -> build.fail() }
        should("accept file active") { _, build -> build.run() }
    }

    context("stonecutter flags") {
        should("not apply to build") { _, build -> build.fail() }
        should("apply before init") { _, build -> build.run() }
        should("override default receiver") { _, build -> build.run("stonecutterGenerate") }
    }

    context("task operations") {
        should("depend on uninitialized tasks") { _, build ->
            with(build.run("sayHelloAndGoodbye").output) {
                shouldContain("Hello!")
                shouldContainOnlyOnce("Goodbye!")
                lastIndexOf("Hello") shouldBeLessThan indexOf("Goodbye!")
            }
        }
    }

    context("file handlers") {
        should("inherit custom handler") { dir, build ->
            build.run("stonecutterSwitchTo2")
            dir read "src/main/example.custom" shouldContain "#comment"
        }

        should("override existing handler") { dir, build ->
            build.run("stonecutterSwitchTo2")
            dir read "src/main/java/Example.java" shouldContain "//int one = 1;"
        }

        should("throw on invalid handler") { _, build -> build.fail() }
    }
})