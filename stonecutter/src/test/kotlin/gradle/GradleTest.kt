package gradle

import dev.kikugie.commons.takeAs
import io.kotest.core.TestConfiguration
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.spec.style.scopes.FreeSpecContainerScope
import io.kotest.core.spec.style.scopes.FreeSpecContextConfigBuilder
import io.kotest.core.test.TestScope
import io.kotest.datatest.withData
import io.kotest.engine.spec.tempdir
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
import kotlin.io.path.readText
import kotlin.io.path.writeText

private fun TestConfiguration.prepareParameters(name: String, gradle: GradleVersions.GradleDistribution?): Pair<Path, GradleTest.Runner> {
    val project = "${this::class.simpleName}/$name"
    val version = gradle?.run { "-$version" } ?: ""
    val dir = tempdir(suffix = project.replace('/', '-') + version, keepOnFailure = true)
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
    gradle?.apply(build)

    val runner = GradleTest.Runner {
        build.withArguments(defaultArgs + it).build()
    }

    return dir.toPath() to runner
}

@OptIn(ExperimentalPathApi::class)
private fun copyResources(project: String, destination: Path) {
    val source = Path("src/test/resources/projects/$project")
    if (source.notExists()) throw FileNotFoundException(source.absolutePathString())

    source.copyToRecursively(destination, followLinks = false, overwrite = true)
}

private fun Set<GradleVersions.GradleDistribution>.toDataMap(): Map<String, GradleVersions.GradleDistribution?> =
    if (isEmpty()) mapOf("Default Gradle" to null) else associateBy { it.version.replace('.', '_') }

@DslMarker @Retention(AnnotationRetention.SOURCE)
private annotation class GradleTestDsl

@GradleTestDsl
context(spec: FreeSpec)
operator fun String.minus(action: suspend TestScope.(directory: Path, build: GradleTest.Runner) -> Unit) = with(spec) {
    this@minus - { build(this@minus.replace(' ', '_').replace('-', '_'), action) }
}

@GradleTestDsl
context(spec: FreeSpec)
operator fun FreeSpecContextConfigBuilder.minus(action: suspend TestScope.(directory: Path, build: GradleTest.Runner) -> Unit) = with(spec) {
    this@minus - { build(name.replace(' ', '_').replace('-', '_'), action) }
}

@GradleTestDsl
context(spec: FreeSpec)
suspend fun FreeSpecContainerScope.build(name: String, action: suspend TestScope.(directory: Path, build: GradleTest.Runner) -> Unit) {
    val data = spec.takeAs<GradleTest>().gradle.toDataMap()
    withData(data) {
        val (dir, runner) = spec.prepareParameters(name, it)
        action(dir, runner)
    }
}

@GradleTestDsl
context(_: FreeSpec)
infix fun Path.write(text: CharSequence) {
    createParentDirectories()
    writeText(text, Charsets.UTF_8, StandardOpenOption.CREATE_NEW)
}

@GradleTestDsl
context(_: FreeSpec)
infix fun Path.read(file: String): String =
    resolve(file).readText()

@GradleTestDsl
interface GradleTest {
    val gradle: Set<GradleVersions.GradleDistribution>
        get() = if (isFull) GradleVersions.ALL else emptySet()

    fun interface Runner {
        fun run(vararg args: String): BuildResult
    }

    private companion object {
        val isFull = System.getProperty("dev.kikugie.stonecutter.full-test", "false").toBoolean()
    }
}