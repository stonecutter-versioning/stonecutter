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
import kotlin.io.path.createParentDirectories
import kotlin.io.path.notExists
import kotlin.io.path.writeText

@DslMarker @Retention(AnnotationRetention.BINARY)
private annotation class GradleTestDsl

@GradleTestDsl
interface GradleProjectTest {
    fun TestConfiguration.build(
        name: String,
        vararg args: String,
        action: (directory: Path, build: Runner) -> Unit
    ) {
        val (dir, runner) = prepareParameters(name, *args)
        action(dir, runner)
    }

    suspend fun TestConfiguration.sbuild(
        name: String,
        vararg args: String,
        action: suspend CoroutineScope.(directory: Path, build: Runner) -> Unit
    ): Unit = coroutineScope {
        val (dir, runner) = prepareParameters(name, *args)
        action(dir, runner)
    }

    infix fun Path.write(text: CharSequence) {
        createParentDirectories()
        writeText(text, Charsets.UTF_8, StandardOpenOption.CREATE_NEW)
    }

    private fun TestConfiguration.prepareParameters(name: String, vararg args: String): Pair<Path, Runner> {
        val project = "${this::class.simpleName}/$name"
        val dir = tempdir(suffix = project.replace('/', '-'), keepOnFailure = true)
        println("Running $project in file://$dir")
        copyResources(project, dir.toPath())

        val defaultArgs = mutableListOf<String>()
        defaultArgs += "--stacktrace"
        defaultArgs += "--parallel"
        defaultArgs += "--rerun-tasks"
        defaultArgs += "--configuration-cache"

        val build = GradleRunner.create()
            .withProjectDir(dir)
            .withPluginClasspath()
            .forwardOutput()

        val runner = Runner {
            build.withArguments(defaultArgs + it).build()
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