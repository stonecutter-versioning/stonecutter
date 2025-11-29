package dev.kikugie.stonecutter.settings.tree

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.ProjectReference
import dev.kikugie.stonecutter.controller.StonecutterControllerManager
import dev.kikugie.stonecutter.data.ProjectHierarchy.Companion.hierarchy
import dev.kikugie.stonecutter.data.StonecutterProject
import dev.kikugie.stonecutter.settings.StonecutterSettingsImpl
import dev.kikugie.stitcher.util.isValidIdentifier
import org.gradle.api.Action
import org.gradle.api.initialization.ProjectDescriptor
import org.gradle.api.initialization.Settings
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.newInstance
import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.notExists

internal abstract class TreeBuilderImpl @Inject constructor(val objects: ObjectFactory) : TreeBuilder() {
    internal var localBuildScriptProvider: ((Identifier, StonecutterProject) -> String)? = null
        private set
    internal val branchBuilders: MutableMap<Identifier, BranchBuilderImpl> = mutableMapOf()
    internal val knownVersions: MutableMap<Identifier, StonecutterProject> = mutableMapOf()

    init {
        branchScript.disallowChanges()
    }

    override fun mapBuilds(mapping: (branch: Identifier, node: StonecutterProject) -> String) {
        localBuildScriptProvider = mapping
    }

    override fun branch(name: Identifier, config: Action<BranchBuilder>) {
        require(name.isEmpty() || name.isValidIdentifier()) { "Invalid branch name: '$name'" }
        getOrCreateBranch(name).let(config::execute)
    }

    override fun versions(entries: List<StonecutterProject>): NodeBuilder =
        getOrCreateBranch("").versions(entries)

    internal fun getVcsProject() = knownVersions[resolveVcs()]!!

    internal fun getRootNodes(): List<NodeBuilderImpl> =
        checkNotNull(branchBuilders[""]?.nodeBuilders) { "Root branch is not initialised yet" }

    internal fun registerVariant(version: StonecutterProject): Unit = with(version) {
        val existing = knownVersions.putIfAbsent(project, this)
        require(existing == null || existing == this) { "Project '$project' is registered with a different version '${existing?.version}'" }
    }

    internal fun createWith(ext: StonecutterSettingsImpl, ref: ProjectReference) {
        val project = ref.resolve(ext.settings)
        ext.treeBuilderContainer[project.hierarchy] = this

        val vcs = resolveVcs()
        val controller = controllerType(ext, project)
        with(project.projectDir.resolve(controller.filename).toPath()) {
            project.buildFileName = ext.checkGroovy(fileName.toString())
            if (notExists()) controller.create(this, vcs)
        }

        for (it in branchBuilders.values)
            it.createWith(ext, project)
    }

    private fun getOrCreateBranch(name: String): BranchBuilderImpl =
        branchBuilders.getOrPut(name) { objects.newInstance<BranchBuilderImpl>(name, this) }

    private fun resolveVcs(): String = vcsVersion.orNull
        ?.also { check(it in knownVersions.keys) { "VCS version '$it' doesn't match any registered subproject" } }
        ?: knownVersions.keys.firstOrNull()
        ?: error("No versions have been registered")

    private fun controllerType(ext: StonecutterSettingsImpl, project: ProjectDescriptor): StonecutterControllerManager {
        val isKotlin = kotlinController.orNull
            ?: ext.kotlinController.orNull
            ?: getDefaultBuildscript(ext, project.projectDir.toPath(), "stonecutter").endsWith("kts")
        return if (isKotlin) StonecutterControllerManager.Kotlin
        else StonecutterControllerManager.Groovy
    }
}

internal abstract class BranchBuilderImpl @Inject constructor(
    val name: String,
    val tree: TreeBuilderImpl,
    val objects: ObjectFactory
) : BranchBuilder() {
    internal val nodeBuilders: MutableList<NodeBuilderImpl> = mutableListOf()

    override fun inherit() {
        nodeBuilders += tree.getRootNodes()
    }

    override fun versions(entries: List<StonecutterProject>): NodeBuilder {
        require(entries.isNotEmpty()) { "No versions provided" }
        for (it in entries) tree.registerVariant(it)
        return objects.newInstance<NodeBuilderImpl>(entries, this).also(nodeBuilders::add)
    }

    internal fun allProjects(): List<StonecutterProject> =
        nodeBuilders.flatMap(NodeBuilderImpl::versions).distinct()

    internal fun createWith(ext: StonecutterSettingsImpl, root: ProjectDescriptor) {
        check(nodeBuilders.isNotEmpty()) { "Branch '$name' has no registered nodes" }
        val project = if (name.isEmpty()) root else name.resolve(ext.settings)
            .apply { projectDir.toPath().createDirectories() }
        if (project.path != root.path)
            project.buildFileName = ext.checkGroovy(resolveBuild(ext, project))

        for ((data, buildscript) in flattenNodes(ext, project))
            createNode(ext, project, data, buildscript)
    }

    private fun createNode(ext: StonecutterSettingsImpl, parent: ProjectDescriptor, data: StonecutterProject, buildscript: String) {
        val project = "${parent.path}:${data.project}".resolve(ext.settings)
        val directory = parent.projectDir.resolve("versions/${data.project}")
            .apply { toPath().createDirectories() }
        with(project) {
            projectDir = directory
            buildFileName = ext.checkGroovy("../../$buildscript")
        }
    }

    private fun resolveBuild(ext: StonecutterSettingsImpl, project: ProjectDescriptor): String = branchScript.orNull
        ?.also { check(!it.startsWith("stonecutter.gradle")) { "Branch buildscript can't match the controller name" } }
        ?: getDefaultBuildscript(ext, project.projectDir.toPath(), "branch")

    private fun flattenNodes(ext: StonecutterSettingsImpl, project: ProjectDescriptor): Map<StonecutterProject, String> = buildMap {
        val branch = project.projectDir.toPath()
        for (builder in nodeBuilders) for (data in builder.versions) {
            val buildscript = builder.buildscript.orNull
                ?: tree.localBuildScriptProvider?.invoke(name, data)
                ?: tree.centralScript.orNull
                ?: ext.centralScript.orNull
                ?: getDefaultBuildscript(ext, branch, "build")
            this[data] = buildscript
        }
    }
}

internal abstract class NodeBuilderImpl @Inject constructor(
    val versions: List<StonecutterProject>,
    val branch: BranchBuilderImpl
) : NodeBuilder()

private fun getDefaultBuildscript(ext: StonecutterSettingsImpl, dir: Path, type: String) = when {
    dir.resolve("$type.gradle.kts").exists() -> "$type.gradle.kts"
    dir.resolve("$type.gradle").exists() || ext.isHardMode -> "$type.gradle"
    else -> "$type.gradle.kts"
}

private operator fun Settings.plus(path: String): ProjectDescriptor {
    val trimmed = path.trimStart(':')
    if (trimmed.isNotEmpty()) include(trimmed)
    return project(":$trimmed")
}

private tailrec fun ProjectReference.resolve(settings: Settings): ProjectDescriptor = when (this) {
    is CharSequence -> settings + toString()
    is ProjectDescriptor -> settings + path
    is Provider<*> -> get().resolve(settings)
    else -> throw IllegalArgumentException("Unsupported project type ${this::class.qualifiedName}")
}