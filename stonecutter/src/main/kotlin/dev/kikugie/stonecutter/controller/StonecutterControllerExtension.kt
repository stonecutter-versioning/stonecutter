package dev.kikugie.stonecutter.controller

import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import dev.kikugie.stonecutter.controller.file.FileHandlerContainer
import dev.kikugie.stonecutter.controller.flag.StonecutterFlags
import dev.kikugie.stonecutter.controller.task.StonecutterControllerTasks
import dev.kikugie.stonecutter.data.StonecutterProject
import dev.kikugie.stonecutter.data.tree.ProjectTree
import dev.kikugie.stonecutter.data.version.SemanticOperations
import dev.kikugie.stonecutter.data.version.VersionOperations
import dev.kikugie.semver.data.SemanticVersion
import dev.kikugie.semver.data.Version
import org.gradle.api.Action

@DslMarker @Retention(AnnotationRetention.BINARY)
private annotation class ControllerDsl

/**
 * Extension interface applied to `stonecutter.gradle(.kts)`.
 *
 * The extension provides active version configuration, task aggregation utilities,
 * and custom file format registration.
 *
 * @see <a href="https://stonecutter.kikugie.dev/wiki/config/params">Wiki #1</a>
 * @see <a href="https://stonecutter.kikugie.dev/wiki/config/settings">Wiki #2</a>
 */
@ControllerDsl
public interface StonecutterControllerExtension : VersionOperations<Version> {
    /**The associated project tree.*/
    public val tree: ProjectTree

    /**The active version. Must be assigned with the [active] function.*/
    public val current: StonecutterProject? get() = tree.current

    /**The version representing the version control reset point.*/
    public val vcsVersion: StonecutterProject get() = tree.vcs

    /**All unique versions across all branches.*/
    public val versions: Set<StonecutterProject> get() = tree.versions

    /**Utility extension for strictly parsing and comparing semantic versions.*/
    public val semantics: VersionOperations<SemanticVersion> get() = SemanticOperations

    /**Immutable Stonecutter configuration flag extension. **Should be configured before the [active] call**.*/
    public val flags: StonecutterFlags

    /**Stonecutter version switch tasks and aggregation utility extension.*/
    public val tasks: StonecutterControllerTasks

    /**Custom file format descriptor registry extension.*/
    public val handlers: FileHandlerContainer

    /**
     * Initializes the plugin and declares an active version.
     * **Must be called exactly once**.
     *
     * The [provider] can be:
     * - A **literal** [String] passed directly to the function.
     * - A [File][java.io.File] containing the active version text.
     * - `null`, which initializes the plugin in detached source mode.
     *
     * TODO: Add wiki link
     */
    public infix fun active(provider: Any?)

    /**
     * Configures the [StonecutterBuildExtension] for all subprojects.
     *
     * This function should be used instead of [subprojects { }][org.gradle.api.Project.subprojects],
     * as it doesn't influence project evaluation order.
     */
    public infix fun parameters(action: Action<StonecutterBuildExtension>)

    /**Configures the [flags] extension.*/
    public infix fun flags(action: Action<StonecutterFlags>): Unit = action.execute(flags)

    /**Configures the [tasks] extension.*/
    public infix fun tasks(action: Action<StonecutterControllerTasks>): Unit = action.execute(tasks)

    /**Configures the [handlers] extension.*/
    public infix fun handlers(action: Action<FileHandlerContainer>): Unit = action.execute(handlers)
}