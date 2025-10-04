package dev.kikugie.stonecutter.controller

import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.StonecutterPlugin
import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import dev.kikugie.stonecutter.controller.StonecutterControllerManager.Companion.getController
import dev.kikugie.stonecutter.controller.ext.MutableFlagContainer
import dev.kikugie.stonecutter.controller.file.FileHandlerBuilder
import dev.kikugie.stonecutter.controller.file.FileHandlerContainer
import dev.kikugie.stonecutter.controller.file.Presets
import dev.kikugie.stonecutter.controller.file.ScannerBuilder
import dev.kikugie.stonecutter.controller.file.StonecutterExperimentalFilesAPI
import dev.kikugie.stonecutter.controller.flag.StonecutterFlag
import dev.kikugie.stonecutter.controller.flag.StonecutterFlags
import dev.kikugie.stonecutter.controller.tasks.StonecutterControllerTasksImpl
import dev.kikugie.stonecutter.data.ProjectHierarchy
import dev.kikugie.stonecutter.data.ProjectHierarchy.Companion.hierarchy
import dev.kikugie.stonecutter.data.StonecutterProject
import dev.kikugie.stonecutter.data.container.BuildPropertiesContainer
import dev.kikugie.stonecutter.data.container.ProjectNodeContainer
import dev.kikugie.stonecutter.data.container.TaskCacheContainer
import dev.kikugie.stonecutter.data.container.TreeBuilderContainer
import dev.kikugie.stonecutter.data.container.getContainer
import dev.kikugie.stonecutter.data.dsl.VersionOperations
import dev.kikugie.stonecutter.data.dsl.impl.LenientOperations
import dev.kikugie.stonecutter.data.tree.builder.BranchBuilderImpl
import dev.kikugie.stonecutter.data.tree.builder.TreeBuilderImpl
import dev.kikugie.stonecutter.data.tree.model.ActiveInfo
import dev.kikugie.stonecutter.data.tree.struct.ProjectBranchImpl
import dev.kikugie.stonecutter.data.tree.struct.ProjectNodeImpl
import dev.kikugie.stonecutter.data.tree.struct.ProjectTreeImpl
import dev.kikugie.stonecutter.process.SCIdeaConfigTask
import dev.kikugie.stonecutter.util.ActiveProvider
import dev.kikugie.stonecutter.util.isIdeaSync
import dev.kikugie.stonecutter.util.requestTasks
import dev.kikugie.stonecutter.util.service
import dev.kikugie.stonecutter.util.set
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.named
import dev.kikugie.semver.data.Version as ParsedVersion

@OptIn(StonecutterInternalAPI::class, StonecutterExperimentalFilesAPI::class)
internal abstract class StonecutterControllerImpl(val root: Project) :
    StonecutterControllerExtension, VersionOperations<ParsedVersion> by LenientOperations {
    private val nodes by lazy { root.gradle.getContainer<ProjectNodeContainer>() }
    private val properties by lazy { root.gradle.getContainer<BuildPropertiesContainer>() }
    private var hasInitialized: Boolean = false
    internal var activeInfo: ActiveInfo = ActiveInfo.empty()
        private set

    override val tree: ProjectTreeImpl =
        constructTree()
    override val tasks: StonecutterControllerTasksImpl =
        StonecutterControllerTasksImpl(this)
    override val flags: MutableFlagContainer =
        MutableFlagContainer(StonecutterFlags { root.findProperty("dev.kikugie.stonecutter.${it.key}")?.toString() })
    override val handlers: FileHandlerContainer =
        root.gradle.service<TaskCacheContainer>("stonecutter-cache").handlers

    init {
        nodes += tree
        configureProject()
        configureSyncTask()
        configureModelTasks()
        configureFileHandlers()
    }

    override fun active(provider: Any?) = initializePluginConfiguration(provider)

    override fun parameters(action: StonecutterBuildExtension.() -> Unit) {
        properties[tree] = action
    }

    private fun configureProject() = root.afterEvaluate {
        if (!hasInitialized) error("Stonecutter branch root $hierarchy has not been initialized. Use `stonecutter.init()` or `stonecutter.active()` to initialize it.")
        if (plugins.hasPlugin("java")) logger.warn("Stonecutter branch root $hierarchy should not be a buildable project. Remove the `java` plugin to fix the issue.")
    }

    private fun initializePluginConfiguration(active: Any?) {
        check(!hasInitialized) { "The plugin has already been initialized!" }
        fun findByName(name: String): StonecutterProject = checkNotNull(tree.versions.find { it.project == name }) {
            "Version '$name' is not registered. This might've been caused by removing a version that is set to be active."
        }

        ActiveProvider.of(active)
            .ifString { name ->
                tree.current = findByName(name)
                activeInfo = ActiveInfo.of(name)
                val controller = checkNotNull(root.getController()) { "Tree ${tree.hierarchy} has no Stonecutter controller" }
                for (it in tree.versions) tasks.registerSelfSwitchTask(it.project, controller)
            }
            .ifFile { file ->
                tree.current = findByName(file.readText().trim())
                activeInfo = ActiveInfo.of(file.toPath())
                for (it in tree.versions) tasks.registerExternalSwitchTask(it.project, file)
            }

        if (flags[StonecutterFlag.APPLY_PLUGIN_TO_NODES]) for (it in tree.nodes)
            it.project.plugins.apply(StonecutterPlugin::class)

        if (tree.current != null) with(root) {
            tasks.register("Reset active project") {
                group = "stonecutter"
                description = "Sets active version to ${tree.vcs.project}. Run this before making a commit."
                dependsOn("${root.hierarchy.orBlank()}:${this@StonecutterControllerImpl.tasks.switchTaskName(tree.vcs.project)}")
            }

            tasks.register("Refresh active project") {
                group = "stonecutter"
                description = "Runs the comment processor on the active version. Useful for fixing comments in wrong states."
                dependsOn("${root.hierarchy.orBlank()}:${this@StonecutterControllerImpl.tasks.switchTaskName(tree.current!!.project)}")
            }

            for (it in tree.versions) tasks.register("Set active project to ${it.project}") {
                group = "stonecutter"
                description = "Sets the active project to ${it.project}, processing all versioned comments."
                dependsOn("${root.hierarchy.orBlank()}:${this@StonecutterControllerImpl.tasks.switchTaskName(it.project)}")
            }
        }

        hasInitialized = true
    }

    private fun configureSyncTask() = root.afterEvaluate {
        if (flags[StonecutterFlag.GENERATE_SWITCH_ACTIONS]) rootProject.tasks.named<SCIdeaConfigTask>("stonecutterIdea") {
            versions[tree.hierarchy.toString()] = tree.versions.map(StonecutterProject::project)
        }

        if (flags[StonecutterFlag.SERIALIZE_TREE_MODEL] && isIdeaSync)
            root.gradle.requestTasks(listOf("stonecutterSaveModels"), root.path, root.projectDir)
    }

    private fun configureModelTasks() = with(tasks) {
        registerModelGroupingTask()
        registerTreeModelTask()
        for (branch in tree.branches) registerBranchModelTask(branch)
    }

    private fun configureFileHandlers() {
        handlers.configureIfAbsent("java") {
            comment(Presets.Commenter.SlashStarNested)
            uncomment(Presets.Uncommenter.DoubleSlashStar)
            scanner { from(Presets.Scanner.DoubleSlashStar) }
        }

        handlers.configureIfAbsent("kt", "kts") {
            comment(Presets.Commenter.SlashStarFlat)
            uncomment(Presets.Uncommenter.DoubleSlashStar)
            scanner { from(Presets.Scanner.DoubleSlashStarNested) }
        }
    }

    private fun constructTree(): ProjectTreeImpl {
        val builder = checkNotNull(root.gradle.getContainer<TreeBuilderContainer>()[root] as? TreeBuilderImpl) {
            "Project ${root.path} is not registered. This might've been caused by removing a project while it's active"
        }
        val branches = builder.constructBranches(root.hierarchy)
        return ProjectTreeImpl(root.gradle, root.hierarchy, builder.getVcsProject(), branches).apply {
            for (it in branches) it.tree = this
        }
    }

    private fun TreeBuilderImpl.constructBranches(tree: ProjectHierarchy) = branches.values.map {
        val nodes = it.constructNodes(tree + it.name)
        ProjectBranchImpl(root.gradle, tree + it.name, it.name, nodes).apply {
            for (node in nodes) node.branch = this
        }
    }

    private fun BranchBuilderImpl.constructNodes(branch: ProjectHierarchy) = allProjects().map {
        ProjectNodeImpl(root.gradle, branch + it.project, it)
    }
}