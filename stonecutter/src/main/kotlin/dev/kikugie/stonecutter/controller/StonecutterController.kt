package dev.kikugie.stonecutter.controller

import dev.kikugie.stonecutter.*
import dev.kikugie.stonecutter.data.ProjectHierarchy
import dev.kikugie.stonecutter.data.ProjectHierarchy.Companion.hierarchy
import dev.kikugie.stonecutter.data.StonecutterProject
import dev.kikugie.stonecutter.data.parameters.BuildParameters
import dev.kikugie.stonecutter.data.parameters.GlobalParameters
import dev.kikugie.stonecutter.data.tree.*
import dev.kikugie.stonecutter.ide.IdeaSetupTask
import dev.kikugie.stonecutter.process.StonecutterTask
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.named
import kotlin.io.path.deleteIfExists

/**
 * Stonecutter plugin applied to `stonecutter.gradle[.kts]`.
 */
@SCDocumentation("controller")
public open class StonecutterController(root: Project) :
    ControllerAbstraction(root),
    StonecutterUtility,
    GlobalParametersAccess {
    override var generateRunConfigs: Collection<RunConfigType> = setOf(RunConfigType.SWITCH, RunConfigType.CHISEL)
    override var debug: Boolean by parameters.named("debug")
    override var processFiles: Boolean by parameters.named("process")
    override var defaultReceiver: Identifier by parameters.named("receiver") {
        require(it.isValid()) { "Invalid receiver '$it'" }
    }

    init {
        prepareConfiguration()
        createDelegateTasks()
        root.afterEvaluate { configureProject() }
    }

    @Suppress("UNCHECKED_CAST")
    private fun prepareConfiguration() = with(StonecutterPlugin.SERVICE()) {
        val maps: Array<MutableMap<ProjectHierarchy, Any>> = arrayOf(mutableMapOf(), mutableMapOf(), mutableMapOf())
        for (it in buildSet {
            add(tree.hierarchy)
            addAll(tree.branches.map { it.hierarchy })
            addAll(tree.versions.flatMap { v -> tree.branches.map { it.hierarchy + v.project } })
        }) {
            maps[0][it] = tree.light
            maps[1][it] = BuildParameters()
            maps[2][it] = this@StonecutterController.parameters
        }
        parameters.projectTrees.putAll(maps[0] as Map<ProjectHierarchy, LightTree>)
        parameters.buildParameters.putAll(maps[1] as Map<ProjectHierarchy, BuildParameters>)
        parameters.globalParameters.putAll(maps[2] as Map<ProjectHierarchy, GlobalParameters>)

        val syncTask = root.tasks.create("chiseledStonecutter")
        for (it in tree.nodes) {
            it.project.pluginManager.apply(StonecutterPlugin::class.java)
            syncTask.dependsOn("${it.hierarchy}:setupChiseledBuild")
        }
    }

    private fun createDelegateTasks() {
        for (it in versions) root.tasks.register<StonecutterTask>("stonecutterSwitchTo${it.project}") {
            group = "stonecutter-impl"
            description = "Internal Stonecutter task. Do not call manually."

            instance(project.hierarchy)

            fromVersion(current)
            toVersion(it)

            input("src")
            output("src")
            sources.set(tree.branches.map { it.light })

            parameters(StonecutterPlugin.SERVICE().snapshot())
            doLast { updateController(it) }
        }
    }

    private fun configureProject() {
        check(tree.configured) { "Active version has not been set!" }

        for (it in tree.nodes) {
            val plugin = it.stonecutter
            configurations[it.branch to it.metadata]?.run { plugin.from(this) }
            builds.onEach { execute(plugin) }
        }

        createStonecutterTask("Reset active project", tree.vcs) {
            "Sets active version to ${tree.vcs.project}. Run this before making a commit."
        }
        createStonecutterTask("Refresh active project", tree.current) {
            "Runs the comment processor on the active version. Useful for fixing comments in wrong states."
        }
        for (it in versions) createStonecutterTask("Set active project to ${it.project}", it) {
            "Sets the active project to ${it.project}, processing all versioned comments."
        }

        serializeTree()
        serializeBranches()
        configureIdeaTask()
    }

    private fun configureIdeaTask() {
        root.rootProject.tasks.named<IdeaSetupTask>("stonecutterIdea") {
            if (RunConfigType.SWITCH in generateRunConfigs)
                versions.put(tree.hierarchy, tree.versions.map(StonecutterProject::project))

            if (RunConfigType.CHISEL in generateRunConfigs)
                tasks.put(tree.hierarchy, parameters.chiseled)
        }
    }

    private fun createStonecutterTask(name: String, version: StonecutterProject, desc: () -> String) = root.tasks.register(name) {
        group = "Stonecutter"
        description = desc()

        dependsOn("${ProjectHierarchy(root.path).orBlank()}:stonecutterSwitchTo${version.project}")
    }

    private fun serializeTree() = with(tree) {
        TreeModel(
            STONECUTTER,
            vcsVersion.project,
            current.project,
            branches.map { BranchInfo(it.id, it.location) },
            nodes.map { NodeInfo(it.metadata, it.location) },
            parameters
        ).save(tree.location.resolve("build/stonecutter-cache")).onFailure {
            root.logger.warn("Failed to save tree model", it)
        }
    }

    private fun serializeBranches() = tree.branches.onEach {
        location.resolve("build/stonecutter-cache/${NodeModel.FILENAME}").runCatching {
            deleteIfExists()
        }.onFailure {
            root.logger.warn("Failed to delete outdated active version model for '$id'", it)
        }
        BranchModel(id, location, nodes.map { NodeInfo(it.metadata, it.location) })
            .save(tree.location.resolve("build/stonecutter-cache"))
            .onFailure {
                root.logger.warn("Failed to save branch model for '$id'", it)
            }
    }
}