package dev.kikugie.stonecutter.build

import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.build.data.BuildConfigurationContainer
import dev.kikugie.stonecutter.build.data.StonecutterBuildConfiguration
import dev.kikugie.stonecutter.build.data.StonecutterBuildData
import dev.kikugie.stonecutter.build.task.StonecutterBuildTasksImpl
import dev.kikugie.stonecutter.controller.file.FileHandlerContainer
import dev.kikugie.stonecutter.controller.flag.StonecutterFlag
import dev.kikugie.stonecutter.controller.flag.StonecutterFlagsView
import dev.kikugie.stonecutter.controller.tree.ProjectNodeContainer
import dev.kikugie.stonecutter.data.ProjectHierarchy.Companion.hierarchy
import dev.kikugie.stonecutter.data.container.GradleContainerExtension.Companion.getContainer
import dev.kikugie.stonecutter.data.tree.ProjectNode
import dev.kikugie.stonecutter.util.isIdeaSync
import dev.kikugie.stonecutter.util.projectDirectory
import dev.kikugie.stonecutter.util.requestTasks
import dev.kikugie.stonecutter.util.sourceSets
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.kotlin.dsl.newInstance
import javax.inject.Inject

@OptIn(StonecutterInternalAPI::class)
internal abstract class StonecutterBuildImpl private constructor(val project: Project, val config: StonecutterBuildConfiguration) :
    StonecutterBuildExtension by config {
    @Inject constructor(project: Project) : this(project, project.findConfig())
    override val tasks: StonecutterBuildTasksImpl = project.objects.newInstance(this)

    init {
        project.plugins.apply("java")
        project.sourceSets.all {
            tasks.configureSource(this)
            createProcessingTasks(node, config.data, filters, tasks)
        }
        project.refreshConfiguration(flags, tasks)
        tasks.registerNodeModelTask()

        val handlers = project.gradle.getContainer<FileHandlerContainer>().handlers
        filters.include {
            it.isDirectory || it.file.extension.lowercase() in handlers.names
        }
    }
}

private fun Project.findConfig(): StonecutterBuildConfiguration =
    gradle.getContainer<BuildConfigurationContainer>()[findNode()]

private fun Project.findNode(): ProjectNode = checkNotNull(gradle.getContainer<ProjectNodeContainer>()[hierarchy]) {
    "$hierarchy is not a registered Stonecutter node"
}

private fun Project.refreshConfiguration(flags: StonecutterFlagsView, impls: StonecutterBuildTasksImpl) = afterEvaluate {
    if (flags[StonecutterFlag.APPEND_SOURCES_AFTER_EVAL])
        sourceSets.forEach(impls::configureSource)

    if (flags[StonecutterFlag.GENERATE_SOURCES_ON_SYNC] && isIdeaSync) impls.generate.tasks
        .map { "${hierarchy.orBlank()}:$it" }
        .let { gradle.requestTasks(it, path, projectDirectory) }
}

@OptIn(StonecutterInternalAPI::class)
private fun SourceSet.createProcessingTasks(
    node: ProjectNode,
    data: StonecutterBuildData,
    filters: PatternFilterable,
    impls: StonecutterBuildTasksImpl
) {
    val name = name
    val sourceDir = "src/$name"
    val localSource = node.location.resolve(sourceDir).toFile()
    val sharedSource = node.branch.location.resolve(sourceDir).toFile()

    val prepareTask = impls.registerPrepareTask(this) {
        params.set(data)
        root.set(sharedSource)
        source.setFrom(node.branch.project.fileTree(sourceDir).matching(filters))
        destination.set(impls.processedCacheDir.dir(name))
    }

    impls.registerGenerateTask(this) {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        from(sharedSource, prepareTask.map { it.destination })
        exclude { !it.isDirectory && it.relativePath.getFile(localSource).exists() }
        into(impls.generatedSourcesDir.dir(name))
    }

    impls.registerMergeTask(this) {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        from(prepareTask.map { it.destination }, localSource)
        into(sharedSource)
    }
}