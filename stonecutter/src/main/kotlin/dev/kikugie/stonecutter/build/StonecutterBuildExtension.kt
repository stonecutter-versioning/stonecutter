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
import org.gradle.api.Action
import org.gradle.api.tasks.util.PatternFilterable

@DslMarker @Retention(AnnotationRetention.BINARY)
private annotation class BuildDsl

@BuildDsl
public interface StonecutterBuildExtension : VersionOperations<Version> {
    public val node: ProjectNode
    public val branch: ProjectBranch get() = node.branch
    public val tree: ProjectTree get() = branch.tree

    public val active: StonecutterProject? get() = tree.current
    public val current: StonecutterProject get() = node.metadata

    public val versions: Collection<StonecutterProject> get() = branch.versions
    public val semantics: VersionOperations<SemanticVersion> get() = SemanticOperations

    public val swaps: SwapContainer
    public val constants: ConstantContainer
    public val dependencies: DependencyContainer
    public val replacements: ReplacementContainer
    public val filters: PatternFilterable

    public val flags: StonecutterFlagsView
    public val tasks: StonecutterBuildTasks

    public infix fun swaps(action: Action<SwapContainer>): Unit = action.execute(swaps)
    public infix fun constants(action: Action<ConstantContainer>): Unit = action.execute(constants)
    public infix fun dependencies(action: Action<DependencyContainer>): Unit = action.execute(dependencies)
    public infix fun replacements(action: Action<ReplacementContainer>): Unit = action.execute(replacements)

    public infix fun flags(action: Action<StonecutterFlagsView>): Unit = action.execute(flags)
    public infix fun tasks(action: Action<StonecutterBuildTasks>): Unit = action.execute(tasks)
}