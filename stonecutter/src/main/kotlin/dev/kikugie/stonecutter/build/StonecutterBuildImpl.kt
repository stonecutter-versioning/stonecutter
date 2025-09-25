package dev.kikugie.stonecutter.build

import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.build.param.StonecutterBuildParameters
import dev.kikugie.stonecutter.build.param.StonecutterBuildProperties
import dev.kikugie.stonecutter.build.task.StonecutterBuildTasksImpl
import dev.kikugie.stonecutter.controller.flag.StonecutterFlag
import dev.kikugie.stonecutter.data.ProjectHierarchy.Companion.hierarchy
import dev.kikugie.stonecutter.data.container.BuildPropertiesContainer
import dev.kikugie.stonecutter.data.container.ProjectNodeContainer
import dev.kikugie.stonecutter.data.container.getContainer
import dev.kikugie.stonecutter.data.tree.struct.ProjectNode
import dev.kikugie.stonecutter.util.isIdeaSync
import dev.kikugie.stonecutter.util.projectDirectory
import dev.kikugie.stonecutter.util.requestTasks
import dev.kikugie.stonecutter.util.sourceSets
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.SourceSet

private fun Project.findNode(): ProjectNode = checkNotNull(gradle.getContainer<ProjectNodeContainer>()[this]) {
    "$hierarchy is not a registered Stonecutter node"
}

private fun Project.findProperties(): StonecutterBuildProperties =
    gradle.getContainer<BuildPropertiesContainer>()[findNode()]

@OptIn(StonecutterInternalAPI::class)
internal abstract class StonecutterBuildImpl(val project: Project, private val properties: StonecutterBuildProperties) :
    StonecutterBuildExtension by properties {
    constructor(project: Project) : this(project, project.findProperties())
    override val tasks: StonecutterBuildTasksImpl = StonecutterBuildTasksImpl(this)
    internal val params: StonecutterBuildParameters
        get() = properties.params

    init {
        configureProject()
    }

    private fun configureProject() {
        project.plugins.apply("java")
        project.sourceSets.all {
            createProcessingTasks(this)
            tasks.configureSource(this)
        }
        filters.include("**/*.java", "**/*.kt", "**/*.kts", "**/*.groovy", "**/*.gradle", "**/*.scala", "**/*.sc", "**/*.json5", "**/*.hjson")
        tasks.registerNodeModelTask()
        configureTaskDependencies()
    }

    private fun configureTaskDependencies(): Unit = project.afterEvaluate {
        if (flags[StonecutterFlag.APPEND_SOURCES_AFTER_EVAL]) sourceSets.forEach(this@StonecutterBuildImpl.tasks::configureSource)
        if (flags[StonecutterFlag.GENERATE_SOURCES_ON_SYNC] && isIdeaSync) this@StonecutterBuildImpl.tasks.generate.keys.map { "$path:$it" }
            .let { gradle.requestTasks(it, path, projectDir) }
    }

    private fun createProcessingTasks(src: SourceSet) {
        val sourceDir = "src/${src.name}"
        val localSource = project.layout.projectDirectory.asFile.resolve(sourceDir)
        val sharedSource = project.parent!!.layout.projectDirectory.dir(sourceDir)

        val prepareTask = tasks.registerPrepareTask(src) {
            params.set(properties.params)
            root.set(project.parent!!.file(sourceDir))
            source.setFrom(project.parent!!.fileTree(sourceDir).matching(filters))
            tasks.processedCacheDir.resolve(src.name).let(destination::set)
        }

        val generateTask = tasks.registerGenerateTask(src) {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
            from(prepareTask.map { it.destination }, sharedSource)
            exclude { !it.isDirectory && it.relativePath.getFile(localSource).exists() }
            into(tasks.generatedSourcesDir.resolve(src.name))
        }

        tasks.registerMergeTask(src) {
            from(generateTask.map { it.outputs.files })
            into(sharedSource)
        }
    }
}