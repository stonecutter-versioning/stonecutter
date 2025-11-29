package dev.kikugie.stonecutter.settings.tree

import dev.kikugie.stonecutter.AnyVersion
import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.data.StonecutterProject
import org.gradle.api.Action
import org.gradle.api.provider.Property

public abstract class TreeBuilder : BranchBuilder() {
    public abstract val vcsVersion: Property<String>
    public abstract val centralScript: Property<String>
    public abstract val kotlinController: Property<Boolean>

    @Deprecated("Root branch has no parent to inherit from", level = DeprecationLevel.HIDDEN)
    override fun inherit(): Unit = throw UnsupportedOperationException()

    public fun branch(name: Identifier): Unit = branch(name) { inherit() }

    public abstract fun branch(name: Identifier, config: Action<BranchBuilder>)
    public abstract fun mapBuilds(mapping: (branch: Identifier, node: StonecutterProject) -> String)
}

public abstract class BranchBuilder {
    public abstract val branchScript: Property<String>

    public fun version(version: AnyVersion): NodeBuilder =
        versions(listOf(StonecutterProject(version, version)))

    public fun version(project: Identifier, version: AnyVersion): NodeBuilder =
        versions(listOf(StonecutterProject(project, version)))

    public infix fun versions(entries: Map<Identifier, AnyVersion>): NodeBuilder =
        versions(entries.map { (k, v) -> StonecutterProject(k, v) })

    public infix fun versions(versions: Iterable<Identifier>): NodeBuilder =
        versions(versions.map { StonecutterProject(it, it) })

    public fun versions(vararg versions: Identifier): NodeBuilder =
        versions(versions.map { StonecutterProject(it, it) })

    @JvmName("versionPairs")
    public infix fun versions(entries: Iterable<Pair<Identifier, AnyVersion>>): NodeBuilder =
        versions(entries.map { (p, v) -> StonecutterProject(p, v) })

    @JvmName("versionPairs")
    public fun versions(vararg entries: Pair<Identifier, AnyVersion>): NodeBuilder =
        versions(entries.map { (p, v) -> StonecutterProject(p, v) })

    public abstract fun inherit()
    public abstract fun versions(entries: List<StonecutterProject>): NodeBuilder
}

public abstract class NodeBuilder {
    public abstract val buildscript: Property<String>

    public fun buildscript(name: String): NodeBuilder = apply {
        buildscript.set(name)
    }
}