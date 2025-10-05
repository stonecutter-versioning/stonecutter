package gradle

import io.kotest.assertions.throwables.shouldThrow
import org.gradle.testkit.runner.UnexpectedBuildFailure

inline fun shouldFailBuild(block: () -> Unit): UnexpectedBuildFailure = shouldThrow<UnexpectedBuildFailure>(block)