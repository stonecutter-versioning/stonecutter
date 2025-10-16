@file:Suppress("NOTHING_TO_INLINE")

package gradle

import io.kotest.assertions.throwables.shouldThrow
import org.gradle.testkit.runner.UnexpectedBuildFailure

inline fun shouldFailBuild(block: () -> Unit): UnexpectedBuildFailure = shouldThrow<UnexpectedBuildFailure>(block)

inline fun GradleTest.Runner.fail(vararg args: String): UnexpectedBuildFailure = shouldFailBuild { run(*args) }