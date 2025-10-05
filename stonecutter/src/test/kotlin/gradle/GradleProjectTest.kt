@file:OptIn(ExperimentalPathApi::class)

package gradle

import io.kotest.core.TestConfiguration
import io.kotest.engine.spec.tempdir
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.FileNotFoundException
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.copyToRecursively
import kotlin.io.path.notExists
import kotlin.io.path.writeText

@GradleTestDsl
interface GradleProjectTest {
    fun TestConfiguration.build(
        project: String,
        vararg args: String,
        action: (directory: Path, build: Runner) -> Unit
    ) {
        val (dir, runner) = prepareParameters(project, *args)
        action(dir, runner)
    }

    suspend fun TestConfiguration.sbuild(
        project: String,
        vararg args: String,
        action: suspend CoroutineScope.(directory: Path, build: Runner) -> Unit
    ): Unit = coroutineScope {
        val (dir, runner) = prepareParameters(project, *args)
        action(dir, runner)
    }

    infix fun Path.write(text: CharSequence): Unit =
        writeText(text, Charsets.UTF_8, StandardOpenOption.CREATE_NEW)

    private fun TestConfiguration.prepareParameters(project: String, vararg args: String): Pair<Path, Runner> {
        val dir = tempdir(suffix = project.replace('/', '-'), keepOnFailure = true)
        copyResources(project, dir.toPath())

        val defaultArgs = mutableListOf<String>()
        defaultArgs += "--stacktrace"
        defaultArgs += "--parallel"
        defaultArgs += "--rerun-tasks"

        val build = GradleRunner.create()
            .withProjectDir(dir)
            .withPluginClasspath()
            .forwardOutput()

        if (WITH_DEBUG in args)
            build.withDebug(true)

        val runner = Runner {
            build.withArguments(defaultArgs + it)
            if (EXPECT_FAIL in args) build.buildAndFail() else build.build()
        }

        return dir.toPath() to runner
    }

    private fun copyResources(project: String, destination: Path) {
        val source = Path("src/test/resources/projects/$project")
        if (source.notExists()) throw FileNotFoundException(source.absolutePathString())

        source.copyToRecursively(destination, followLinks = false, overwrite = true)
    }

    fun interface Runner {
        fun run(vararg args: String): BuildResult
    }
}