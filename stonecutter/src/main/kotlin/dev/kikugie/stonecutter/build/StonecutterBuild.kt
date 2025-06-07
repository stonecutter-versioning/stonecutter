package dev.kikugie.stonecutter.build

import dev.kikugie.stonecutter.*
import dev.kikugie.stonecutter.data.ProjectHierarchy.Companion.hierarchy
import dev.kikugie.stonecutter.data.StonecutterProject
import dev.kikugie.stonecutter.data.container.ConfigurationService.Companion.of
import dev.kikugie.stonecutter.data.tree.*
import dev.kikugie.stonecutter.process.StonecutterTask
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.register
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.invariantSeparatorsPathString

// link: wiki-build
/**
 * Stonecutter plugin applied to the versioned buildscript.
 *
 * @property project This plugin's project
 * @see <a href="https://stonecutter.kikugie.dev/stonecutter/guide/setup#versioning-build-gradle-kts">Wiki page</a>
 */
@OptIn(ExperimentalPathApi::class)
public open class StonecutterBuild(private val project: Project) : BuildAbstraction(project.hierarchy), StonecutterUtility {
    private val parent: Project = requireNotNull(project.parent) { "No parent project for '${project.hierarchy}'" }

    /**Project tree instance containing the necessary data and safe to use with configuration cache.
     * @see [withProject]*/
    @StonecutterAPI public val tree: ProjectTree = StonecutterPlugin.SERVICE().parameters.projectTrees.getChecked(parent.hierarchy) {
        "Tree for '${project.hierarchy}' not found in ${keysToString()}"
    }.withProject(project)

    /**Branch this node belongs to containing the necessary data and safe to use with configuration cache.
     * @see [withProject]*/
    @StonecutterAPI public val branch: ProjectBranch = tree.getChecked(parent.hierarchy) {
        "Branch for '$it' not found in ${tree.hierarchy}: ${keysToString()}"
    }
    /**This project's node containing only the necessary data and safe to use with configuration cache.
     * @see [withProject]*/
    @StonecutterAPI public val node: ProjectNode = branch.getChecked(project.hierarchy) {
        "Node for '$it' not found in ${branch.hierarchy}: ${keysToString()}"
    }

    /**All versions in this project's branch.*/
    @StonecutterAPI public val versions: Collection<StonecutterProject> get() = branch.versions

    /**The currently active version. Global for all instances of the build file.*/
    @StonecutterAPI public val active: StonecutterProject get() = tree.current

    /**Metadata of the currently processed version.*/
    @StonecutterAPI public val current: StonecutterProject = node.metadata

    init {
        createSetupTask()
        project.afterEvaluate {
            configureSources()
            serializeNode()
        }
    }

    private fun createSetupTask() = project.tasks.register<StonecutterTask>("setupChiseledBuild") {
        val chiseledSrc = project.projectPath.resolve("build/chiseledSrc")
        instance(project.hierarchy)

        fromVersion.set(active)
        toVersion.set(current)

        input("src")
        output(parent.projectPath.relativize(chiseledSrc).invariantSeparatorsPathString)
        sources.set(listOf(branch.light))

        parameters(StonecutterPlugin.SERVICE().snapshot())
        doFirst {
            chiseledSrc
                .runCatching { if (exists()) deleteRecursively() }
                .onFailure { logger.warn("Failed to clean chiseledSrc", it) }
        }
    }

    private fun configureSources() {
        val globalParameters = StonecutterPlugin.SERVICE.of(hierarchy).global
            ?: error("No global parameters for '${hierarchy}'")
        val useChiseledSrc =
            globalParameters.process && globalParameters.hasChiseled(project.gradle.startParameter.taskNames)
        val formatter: (Path) -> Any = when {
            useChiseledSrc -> { src -> project.projectDir.resolve("build/chiseledSrc/$src") }
            current.isActive -> { src -> "../../src/$src" }
            else -> return
        }

        val parentDir = parent.projectDir.resolve("src").toPath()
        val thisDir = project.projectDir.resolve("src").toPath()

        fun applyChiseled(from: SourceDirectorySet, to: SourceDirectorySet = from) = from.sourceDirectories.mapNotNull {
            val relative = thisDir.relativize(it.toPath())
            if (relative.startsWith(".."))
                return@mapNotNull if (current.isActive) null
                else parentDir.relativize(it.toPath())
            else relative
        }.forEach {
            to.srcDir(formatter(it))
        }

        project.sourceSets?.onEach {
            applyChiseled(allJava, java)
            applyChiseled(resources)
            extensions.extensionsSchema
                .filter { it.publicType.concreteClass.interfaces.contains(SourceDirectorySet::class.java) }
                .forEach { applyChiseled(extensions[it.name] as SourceDirectorySet) }
        }
    }

    private fun serializeNode() {
        val model = NodeModel(current.project, current.version, current.isActive, BranchInfo(branch.id, branch.location), node.location, data)
        model.save(node.location.resolve("build/stonecutter-cache")).onFailure {
            project.logger.warn("Failed to save node model for '${branch.id}:${current.project}'", it)
        }
        if (current.isActive) model.save(branch.location.resolve("build/stonecutter-cache")).onFailure {
            project.logger.warn("Failed to save active node model for '${branch.id}:${current.project}'", it)
        }
    }
}