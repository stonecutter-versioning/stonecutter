@file:Suppress("unused", "PublicApiImplicitType", "RemoveRedundantBackticks")
package integration

import gradle.GradleProjectTest
import io.kotest.assertions.nondeterministic.continually
import io.kotest.assertions.nondeterministic.continuallyConfig
import io.kotest.assertions.retry
import io.kotest.assertions.retryConfig
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.shouldBeStrictlyIncreasingWith
import io.kotest.matchers.collections.shouldNotBeStrictlyIncreasingWith
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldContainOnlyOnce
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TaskHookTest : AnnotationSpec(), GradleProjectTest {
    @Test fun `aggregation`() = build("aggregation") { directory, build ->
        val output = build.run("sayHelloAndGoodbye").output
        output shouldContain "Hello!"
        output shouldContainOnlyOnce "Goodbye!"
        output.lastIndexOf("Hello") shouldBeLessThan output.indexOf("Goodbye!")
    }

    @Test suspend fun `ordering`() = sbuild("ordering") { directory, build ->
        fun String.printOrder() = lineSequence()
            .mapNotNull { it.substringAfter("My version is ", "").ifEmpty { return@mapNotNull null }.toInt() }

        continually(continuallyConfig { duration = 30.seconds; interval = 10.seconds }) {
            val result = build.run("printVersion", "-Porder-prints=true").output
            result.printOrder() shouldBeStrictlyIncreasingWith Comparator.naturalOrder()
        }

        retry(retryConfig { maxRetry = 10; delay = 10.seconds; timeout = 1.minutes }) {
            val result = build.run("printVersion").output
            result.printOrder() shouldNotBeStrictlyIncreasingWith Comparator.naturalOrder()
        }
    }
}