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

@ControllerDsl
public interface StonecutterControllerExtension : VersionOperations<Version> {
    public val tree: ProjectTree

    public val current: StonecutterProject? get() = tree.current

    public val vcsVersion: StonecutterProject get() = tree.vcs

    public val versions: Set<StonecutterProject> get() = tree.versions

    public val semantics: VersionOperations<SemanticVersion> get() = SemanticOperations

    public val flags: StonecutterFlags
    public val tasks: StonecutterControllerTasks
    public val handlers: FileHandlerContainer

    public infix fun active(provider: Any?)
    public infix fun parameters(action: Action<StonecutterBuildExtension>)

    public infix fun flags(action: Action<StonecutterFlags>): Unit = action.execute(flags)
    public infix fun tasks(action: Action<StonecutterControllerTasks>): Unit = action.execute(tasks)
    public infix fun handlers(action: Action<FileHandlerContainer>): Unit = action.execute(handlers)
}