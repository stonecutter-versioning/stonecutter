package newintegration

import gradle.GradleTest
import gradle.fail
import gradle.should
import io.kotest.core.spec.style.ShouldSpec

class ControllerExtensionTest : GradleTest, ShouldSpec({
    should("create extension alias") { _, build -> build.run() }
    should("fail with disabled alias") { _, build -> build.fail() }

    context("plugin initialization") {
        should("fail without active call") { _, build -> build.fail() }
        should("fail with base plugin") { _, build -> build.fail() }
        should("validate active parameter") { _, build -> build.fail() }
        should("accept file active") { _, build -> build.run() }
    }
})