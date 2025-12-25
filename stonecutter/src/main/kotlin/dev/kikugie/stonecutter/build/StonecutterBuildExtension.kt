package dev.kikugie.stonecutter.build

import dev.kikugie.stonecutter.build.config.ConstantContainer
import dev.kikugie.stonecutter.build.config.DependencyContainer
import dev.kikugie.stonecutter.build.config.ReplacementContainer
import dev.kikugie.stonecutter.build.config.SwapContainer
import dev.kikugie.stonecutter.build.task.StonecutterBuildTasks
import dev.kikugie.stonecutter.controller.flag.StonecutterFlagsView
import dev.kikugie.stonecutter.data.StonecutterProject
import dev.kikugie.stonecutter.data.tree.ProjectBranch
import dev.kikugie.stonecutter.data.tree.ProjectNode
import dev.kikugie.stonecutter.data.tree.ProjectTree
import dev.kikugie.stonecutter.data.version.SemanticOperations
import dev.kikugie.stonecutter.data.version.VersionOperations
import dev.kikugie.semver.data.SemanticVersion
import dev.kikugie.semver.data.Version
import dev.kikugie.stonecutter.StonecutterExperimentalAPI
import org.gradle.api.Action
import org.gradle.api.tasks.util.PatternFilterable
import java.io.File

@DslMarker @Retention(AnnotationRetention.BINARY)
private annotation class BuildDsl

/**
 * Extension interface applied to versioned Stonecutter buildscripts.
 *
 * The extension provides configuration options for parameters using in file preprocesings,
 * as well as utilities for configuring version-specific buildscript actions.
 *
 * @see <a href="https://stonecutter.kikugie.dev/wiki/config/params">Wiki #1</a>
 * @see <a href="https://stonecutter.kikugie.dev/wiki/config/settings">Wiki #2</a>
 */
@BuildDsl
public interface StonecutterBuildExtension : VersionOperations<Version> {
    /**The associated Stonecutter project node.*/
    public val node: ProjectNode

    /**The containing Stonecutter project branch.*/
    public val branch: ProjectBranch get() = node.branch

    /**The containing Stonecutter project tree.*/
    public val tree: ProjectTree get() = branch.tree

    /**The active project assigned in `stonecutter.gradle(.kts)`.*/
    public val active: StonecutterProject? get() = tree.current

    /**The project processed in this buildscript instance.*/
    public val current: StonecutterProject get() = node.metadata

    /**All projects registered in the [branch].*/
    public val versions: Collection<StonecutterProject> get() = branch.versions

    /**Utility extension for strictly parsing and comparing semantic versions.*/
    public val semantics: VersionOperations<SemanticVersion> get() = SemanticOperations

    /**[Stonecutter swap](https://stonecutter.kikugie.dev/wiki/config/params#string-swaps) configuration extension.*/
    public val swaps: SwapContainer

    /**[Stonecutter constant](https://stonecutter.kikugie.dev/wiki/config/params#condition-constants) configuration extension.*/
    public val constants: ConstantContainer

    /**[Stonecutter dependency](https://stonecutter.kikugie.dev/wiki/config/params#condition-dependencies) configuration extension.*/
    public val dependencies: DependencyContainer

    /**[Stonecutter replacement](https://stonecutter.kikugie.dev/wiki/config/params#replacements) configuration extension.*/
    public val replacements: ReplacementContainer

    /**Per-[SourceSet][org.gradle.api.tasks.SourceSet] file filters. **Should only be used to exclude files**.*/
    public val filters: PatternFilterable

    /**Immutable Stonecutter configuration flag extension.*/
    public val flags: StonecutterFlagsView

    /**Stonecutter file processing task access extension.*/
    public val tasks: StonecutterBuildTasks

    /**Configures [Stonecutter swaps](https://stonecutter.kikugie.dev/wiki/config/params#string-swaps).*/
    public infix fun swaps(action: Action<SwapContainer>): Unit = action.execute(swaps)

    /**Configures [Stonecutter constants](https://stonecutter.kikugie.dev/wiki/config/params#condition-constants).*/
    public infix fun constants(action: Action<ConstantContainer>): Unit = action.execute(constants)

    /**Configures [Stonecutter dependencies](https://stonecutter.kikugie.dev/wiki/config/params#condition-dependencies).*/
    public infix fun dependencies(action: Action<DependencyContainer>): Unit = action.execute(dependencies)

    /**Configures [Stonecutter replacements](https://stonecutter.kikugie.dev/wiki/config/params#replacements).*/
    public infix fun replacements(action: Action<ReplacementContainer>): Unit = action.execute(replacements)

    /**Configures the [flags] extension.*/
    public infix fun flags(action: Action<StonecutterFlagsView>): Unit = action.execute(flags)

    /**Configures the [tasks] extension.*/
    public infix fun tasks(action: Action<StonecutterBuildTasks>): Unit = action.execute(tasks)

    /**
     * Processes the [file] using existing handlers and configuration,
     * writing the result to [destination] relative to the project directory.
     * @return The processed file
     */
    @StonecutterExperimentalAPI
    public fun process(file: File, destination: String): File
}