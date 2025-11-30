package gradle

import com.github.ajalt.mordant.rendering.TextColors
import io.kotest.core.TestConfiguration
import io.kotest.core.test.TestScope
import io.kotest.engine.spec.tempdir
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.FileNotFoundException
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.*

@DslMarker @Retention(AnnotationRetention.SOURCE)
annotation class GradleTestDsl

@GradleTestDsl
interface GradleTest {
    fun interface Runner {
        fun run(vararg args: String): BuildResult
    }
}

infix fun Path.read(file: String): String =
    resolve(file).readText()

infix fun Path.write(text: CharSequence) {
    createParentDirectories()
    writeText(text, Charsets.UTF_8, StandardOpenOption.CREATE_NEW)
}

suspend inline fun TestConfiguration.run(
    name: String,
    scope: TestScope,
    crossinline action: suspend TestScope.(directory: Path, build: GradleTest.Runner) -> Unit
) {
    val (dir, runner) = prepareParameters(name.replace(' ', '_').replace('-', '_'))
    scope.action(dir, runner)
}

fun TestConfiguration.prepareParameters(name: String): Pair<Path, GradleTest.Runner> {
    val project = "${this::class.simpleName}/$name"
    val source = Path("src/test/resources/projects/$project")
    val dir = tempdir(suffix = project.replace('/', '-'), keepOnFailure = true)
    println(buildString {
        appendLine(TextColors.cyan("### Running test '$project' ###"))
        appendLine(TextColors.cyan("- Source: file://${source.absolutePathString()}"))
        appendLine(TextColors.cyan("- Build: file://${dir.absolutePath}"))
    })
    copyResources(source, dir.toPath())

    val defaultArgs = mutableListOf<String>()
    defaultArgs += "--stacktrace"
    defaultArgs += "--parallel"
    defaultArgs += "--rerun-tasks"
    defaultArgs += "--configuration-cache"

    val build = GradleRunner.create()
        .withProjectDir(dir)
        .withPluginClasspath()
        .forwardStdOutput(StyledPrintWriter(TextColors.white))
        .forwardStdError(StyledPrintWriter(TextColors.red))

    val runner = GradleTest.Runner {
        build.withArguments(defaultArgs + it).build()
    }

    return dir.toPath() to runner
}

@OptIn(ExperimentalPathApi::class)
private fun copyResources(source: Path, destination: Path) {
    if (source.notExists()) throw FileNotFoundException(source.absolutePathString())
    source.copyToRecursively(destination, followLinks = false, overwrite = true)
}