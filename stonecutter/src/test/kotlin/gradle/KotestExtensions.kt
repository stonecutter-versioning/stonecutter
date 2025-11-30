@file:Suppress("NOTHING_TO_INLINE")

package gradle

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.spec.style.scopes.FreeSpecContextConfigBuilder
import io.kotest.core.spec.style.scopes.ShouldSpecContainerScope
import io.kotest.core.test.TestScope
import org.gradle.testkit.runner.UnexpectedBuildFailure
import java.nio.file.Path

inline fun shouldFailBuild(block: () -> Unit): UnexpectedBuildFailure =
    shouldThrow<UnexpectedBuildFailure>(block)

inline fun GradleTest.Runner.fail(vararg args: String): UnexpectedBuildFailure =
    shouldFailBuild { run(*args) }

@GradleTestDsl context(spec: ShouldSpec)
fun should(name: String, action: suspend TestScope.(directory: Path, build: GradleTest.Runner) -> Unit) = spec.should(name) {
    spec.run(name, this, action)
}

@GradleTestDsl context(spec: ShouldSpec)
fun xshould(name: String, action: suspend TestScope.(directory: Path, build: GradleTest.Runner) -> Unit) = spec.xshould(name) {
    spec.run(name, this, action)
}

@GradleTestDsl context(spec: ShouldSpec)
suspend fun ShouldSpecContainerScope.should(name: String, action: suspend TestScope.(directory: Path, build: GradleTest.Runner) -> Unit) = should(name) {
    spec.run(name, this, action)
}

@GradleTestDsl context(spec: ShouldSpec)
suspend fun ShouldSpecContainerScope.xshould(name: String, action: suspend TestScope.(directory: Path, build: GradleTest.Runner) -> Unit) = xshould(name) {
    spec.run(name, this, action)
}

@GradleTestDsl context(spec: FreeSpec)
operator fun String.minus(action: suspend TestScope.(directory: Path, build: GradleTest.Runner) -> Unit) = with(spec) {
    this@minus - { run(this@minus, this, action) }
}

@GradleTestDsl context(spec: FreeSpec)
operator fun FreeSpecContextConfigBuilder.minus(action: suspend TestScope.(directory: Path, build: GradleTest.Runner) -> Unit) = with(spec) {
    this@minus - { run(name, this, action) }
}