package dev.kikugie.stonecutter.controller

import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import dev.kikugie.stonecutter.build.data.BuildConfigurationContainer
import dev.kikugie.stonecutter.controller.StonecutterControllerManager.Companion.getController
import dev.kikugie.stonecutter.controller.file.FileHandlerContainer
import dev.kikugie.stonecutter.controller.flag.StonecutterFlag
import dev.kikugie.stonecutter.controller.flag.StonecutterFlagStorage
import dev.kikugie.stonecutter.controller.flag.StonecutterFlags
import dev.kikugie.stonecutter.controller.flag.StonecutterFlagsImpl
import dev.kikugie.stonecutter.controller.task.StonecutterControllerTasksImpl
import dev.kikugie.stonecutter.controller.tree.ProjectNodeContainer
import dev.kikugie.stonecutter.data.ParsedVersion
import dev.kikugie.stonecutter.data.ProjectHierarchy
import dev.kikugie.stonecutter.data.ProjectHierarchy.Companion.hierarchy
import dev.kikugie.stonecutter.data.StonecutterProject
import dev.kikugie.stonecutter.data.container.GradleContainerExtension.Companion.getContainer
import dev.kikugie.stonecutter.data.tree.ProjectBranchImpl
import dev.kikugie.stonecutter.data.tree.ProjectNodeImpl
import dev.kikugie.stonecutter.data.tree.ProjectTreeImpl
import dev.kikugie.stonecutter.data.version.LenientOperations
import dev.kikugie.stonecutter.data.version.VersionOperations
import dev.kikugie.stonecutter.settings.task.StonecutterIdeaConfigTask
import dev.kikugie.stonecutter.settings.tree.BranchBuilderImpl
import dev.kikugie.stonecutter.settings.tree.TreeBuilderContainer
import dev.kikugie.stonecutter.settings.tree.TreeBuilderImpl
import dev.kikugie.stonecutter.util.isIdeaSync
import dev.kikugie.stonecutter.util.requestTasks
import dev.kikugie.semver.data.Version
import dev.kikugie.stonecutter.StonecutterPlugin
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.newInstance
import java.io.File
import java.nio.file.Path
import javax.inject.Inject

internal abstract class StonecutterControllerImpl @Inject constructor(val root: Project) :
    StonecutterControllerExtension, VersionOperations<Version> by LenientOperations {
    override val tree: ProjectTreeImpl = constructTree(root)
    override val tasks: StonecutterControllerTasksImpl = root.objects.newInstance(this)
    override val flags: StonecutterFlagsImpl = StonecutterFlagStorage { root.findProperty("dev.kikugie.stonecutter.${it.key}")?.toString() }
        .let(::StonecutterFlagsImpl)
    override val handlers: FileHandlerContainer = root.gradle.getContainer()

    private val nodeContainer: ProjectNodeContainer = root.gradle.getContainer<ProjectNodeContainer>().also { it += tree }
    private val configContainer: BuildConfigurationContainer = root.gradle.getContainer()
    private var hasInitialized: Boolean = false

    init {
        root.afterEvaluate {
            if (!hasInitialized)
                error("Stonecutter branch root $hierarchy has not been initialized. Use `stonecutter.init()` or `stonecutter.active()` to initialize it.")

            if (plugins.hasPlugin("base"))
                logger.warn("Stonecutter branch root $hierarchy should not be a buildable project. Remove the `base` or `java` plugin to fix the issue.")
        }
    }

    override fun active(provider: Any?) {
        check(!hasInitialized) { "Stonecutter has already been initialized!" }
        tree.assignActive(root, tasks, provider.resolveActive())

        if (flags[StonecutterFlag.APPLY_PLUGIN_TO_NODES]) for (it in tree.nodes)
            it.project.plugins.apply(StonecutterPlugin::class)

        if (tree.current != null) tree.createSwitchTasks(root, tasks)
        tree.createModelTasks(tasks)
        tree.configureSyncTask(root, flags)

        hasInitialized = true
    }

    override fun parameters(action: Action<StonecutterBuildExtension>) {
        configContainer[tree] = action
    }
}

private fun constructTree(root: Project): ProjectTreeImpl {
    val builder = checkNotNull(root.gradle.getContainer<TreeBuilderContainer>()[root.hierarchy]) {
        "Project ${root.path} is not registered. This might've been caused by removing a project while it's active"
    }
    val branches = builder.constructBranches(root, root.hierarchy)
    return ProjectTreeImpl(root.gradle, root.hierarchy, builder.getVcsProject(), branches).apply {
        for (it in branches) it.tree = this
    }
}

private fun TreeBuilderImpl.constructBranches(root: Project, tree: ProjectHierarchy): List<ProjectBranchImpl> = branchBuilders.values.map {
    val nodes = it.constructNodes(root, tree + it.name)
    ProjectBranchImpl(root.gradle, tree + it.name, it.name, nodes).apply {
        for (node in nodes) node.branch = this
    }
}

private fun BranchBuilderImpl.constructNodes(root: Project, branch: ProjectHierarchy): List<ProjectNodeImpl> = allProjects().map {
    ProjectNodeImpl(root.gradle, branch + it.project, it)
}

private fun ProjectTreeImpl.findByName(name: String): StonecutterProject = checkNotNull(versions.find { it.project == name }) {
    "Version '$name' is not registered. This might've been caused by removing a version that is set to be active."
}

private fun ProjectTreeImpl.assignActive(root: Project, tasks: StonecutterControllerTasksImpl, provider: Any?) = when (provider) {
    is String -> {
        current = findByName(provider)
        val manager = checkNotNull(root.getController()) { "Tree $hierarchy has no Stonecutter controller" }
        for (it in versions) tasks.registerScriptSwitchTask(it.project, manager)
    }

    is File -> {
        current = findByName(provider.readText().trim())
        for (it in versions) tasks.registerExternalSwitchTask(it.project, provider)
    }

    else -> {
    }
}

private fun ProjectTreeImpl.createSwitchTasks(root: Project, impls: StonecutterControllerTasksImpl): Unit = with(root) {
    tasks.register("Reset active project") {
        group = "stonecutter"
        description = "Sets active version to ${vcs.project}. Run this before making a commit."
        dependsOn(impls.switch.getOrThrow(vcs.project))
    }

    tasks.register("Refresh active project") {
        group = "stonecutter"
        description = "Runs the comment processor on the active version. Useful for fixing comments in wrong states."
        dependsOn(impls.switch.getOrThrow(current!!.project))
    }

    val sorting: Comparator<StonecutterProject> = Comparator.comparing<StonecutterProject, ParsedVersion> { it.parsed }.thenComparing { it.project }
    for (it in versions.sortedWith(sorting)) tasks.register("Set active project to ${it.project}") {
        group = "stonecutter"
        description = "Sets the active project to ${it.project}, processing all versioned comments."
        dependsOn(impls.switch.getOrThrow(it.project))
    }
}

private fun ProjectTreeImpl.createModelTasks(impls: StonecutterControllerTasksImpl) {
    val aggregate = impls.registerModelGroupingTask()
    val delegates = buildList {
        this += impls.registerTreeModelTask()
        for (branch in branches)
            this += impls.registerBranchModelTask(branch)
    }

    aggregate.configure {
        dependsOn(delegates)
    }
}

private fun ProjectTreeImpl.configureSyncTask(root: Project, flags: StonecutterFlags) = root.afterEvaluate {
    if (flags[StonecutterFlag.GENERATE_SWITCH_ACTIONS]) rootProject.tasks.named<StonecutterIdeaConfigTask>("stonecutterIdea") {
        projects.put(hierarchy, versions.map(StonecutterProject::project))
    }

    if (flags[StonecutterFlag.SERIALIZE_TREE_MODEL] && isIdeaSync)
        root.gradle.requestTasks(listOf("stonecutterSaveModels"), root.path, root.projectDir)
}

private tailrec fun Any?.resolveActive(): Any? = when(this) {
    null,
    is String,
    is File -> this
    is Path -> toFile()
    is RegularFile -> asFile
    is Provider<*> -> get().resolveActive()
    else -> throw IllegalArgumentException("Unsupported active project type ${this::class.qualifiedName}")
}