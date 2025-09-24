package dev.kikugie.stonecutter.controller

import dev.kikugie.semver.data.SemanticVersion
import dev.kikugie.stonecutter.ActiveReference
import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import dev.kikugie.stonecutter.controller.ext.MutableFlagContainer
import dev.kikugie.stonecutter.controller.tasks.StonecutterControllerTasks
import dev.kikugie.stonecutter.data.StonecutterProject
import dev.kikugie.stonecutter.data.dsl.VersionOperations
import dev.kikugie.stonecutter.data.dsl.impl.SemanticOperations
import dev.kikugie.stonecutter.data.tree.struct.ProjectTree
import org.gradle.api.Action
import org.gradle.api.plugins.ExtensionAware
import dev.kikugie.semver.data.Version as ParsedVersion

@DslMarker @Retention(AnnotationRetention.BINARY)
private annotation class ControllerDsl

/**Stonecutter plugin available in `stonecutter.gradle[.kts]`.*/
@StonecutterAPI @ControllerDsl
public interface StonecutterControllerExtension : ExtensionAware, VersionOperations<ParsedVersion> {
    public val tree: ProjectTree

    /**Active version assigned by [active] function.*/
    public val current: StonecutterProject?
        get() = tree.current

    /**VCS project assigned during tree construction.*/
    public val vcsVersion: StonecutterProject
        get() = tree.vcs

    /**
     * All unique versions in the tree. Unlike versions in branches,
     * these may contain duplicate [StonecutterProject.project] entries.
     */
    public val versions: Collection<StonecutterProject>
        get() = tree.versions


    public val semantics: VersionOperations<SemanticVersion>
        get() = SemanticOperations

    public val flags: MutableFlagContainer
    public val tasks: StonecutterControllerTasks

    /**
     * Initialises the plugin with the given active version.
     * **This function must be called exactly once**.
     *
     * @see ActiveReference
     */
    public infix fun active(provider: ActiveReference)

    /**
     * Configures [stonecutter parameters][dev.kikugie.stonecutter.build.param.StonecutterBuildConfig] for each subproject in the tree.
     *
     * This configuration is preferred to [subprojects {}][org.gradle.api.Project.subprojects],
     * as it's lazily evaluated when the build plugin is applied to the subproject,
     * instead of resolving it immediately.
     */
    public infix fun parameters(config: Action<StonecutterBuildExtension>)
}