package dev.kikugie.stonecutter.build

import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.build.param.StonecutterBuildProperties
import dev.kikugie.stonecutter.build.task.StonecutterBuildTasksImpl
import dev.kikugie.stonecutter.controller.StonecutterControllerExtension
import dev.kikugie.stonecutter.controller.flag.FlagContainer
import dev.kikugie.stonecutter.controller.flag.StonecutterFlag
import dev.kikugie.stonecutter.data.ProjectHierarchy.Companion.hierarchy
import dev.kikugie.stonecutter.data.container.BuildPropertiesContainer
import dev.kikugie.stonecutter.data.container.ProjectNodeContainer
import dev.kikugie.stonecutter.data.container.getContainer
import dev.kikugie.stonecutter.data.dsl.*
import dev.kikugie.stonecutter.data.dsl.impl.LenientOperations
import dev.kikugie.stonecutter.data.tree.struct.ProjectNode
import dev.kikugie.stonecutter.util.*
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet
import org.gradle.kotlin.dsl.the
import dev.kikugie.semver.data.Version as ParsedVersion

@OptIn(StonecutterInternalAPI::class)
// TODO: Merge this and properties
internal abstract class StonecutterBuildImpl(val project: Project) : StonecutterBuildExtension, VersionOperations<ParsedVersion> by LenientOperations {
    internal val parent: Project = checkNotNull(project.parent) { "Stonecutter plugin has been incorrectly applied. Refer to the wiki for a guide." }
    internal val properties: StonecutterBuildProperties by lazy { project.gradle.getContainer<BuildPropertiesContainer>()[node] }

    override val node: ProjectNode by lazy {
        val container = project.gradle.getContainer<ProjectNodeContainer>()
        checkNotNull(container[project]) { "${project.hierarchy} is not a registered Stonecutter node" }
    }
    override val tasks: StonecutterBuildTasksImpl = StonecutterBuildTasksImpl(this)
    override val flags: FlagContainer by lazy { tree.project.the<StonecutterControllerExtension>().flags }
    override val filters: PatternFilterable = PatternSet()

    init {
        configureProject()
    }

    private fun configureProject(): Unit = with(project) {
        plugins.apply("java")
        sourceSets.all {
            createProcessingTasks(this)
            this@StonecutterBuildImpl.tasks.configureSource(this)
        }
        filters.include("**/*.java", "**/*.kt", "**/*.kts", "**/*.groovy", "**/*.gradle", "**/*.scala", "**/*.sc", "**/*.json5", "**/*.hjson")
        this@StonecutterBuildImpl.tasks.registerNodeModelTask()
        configureTaskDependencies()
    }

    private fun configureTaskDependencies(): Unit = project.afterEvaluate {
        if (flags[StonecutterFlag.APPEND_SOURCES_AFTER_EVAL]) sourceSets.forEach(this@StonecutterBuildImpl.tasks::configureSource)
        if (flags[StonecutterFlag.GENERATE_SOURCES_ON_SYNC] && isIdeaSync) this@StonecutterBuildImpl.tasks.generate.keys.map { "$path:$it" }
            .let { gradle.requestTasks(it, path, projectDir) }
    }

    private fun createProcessingTasks(src: SourceSet) {
        val overrides = project.projectDirectory.resolve("src/${src.name}")
        val prepareTask = tasks.registerPrepareTask(src) {
            params.set(properties.params)
            parent.file("src/${src.name}").let(root::set)
            project.provider { parent.fileTree("src/${src.name}").matching(filters) }.let { source.setFrom(it) }
            tasks.processedCacheDir.resolve(src.name).let(destination::set)
        }

        tasks.registerGenerateTask(src) {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
            from(parent.projectDirectory.resolve("src/${src.name}"), tasks.processedCacheDir.resolve(src.name))
            exclude { !it.isDirectory && it.relativePath.getFile(overrides).exists() }
            into(tasks.generatedSourcesDir.resolve(src.name))
            dependsOn(prepareTask)
        }

        tasks.registerMergeTask(src) {
            from(tasks.processedCacheDir.resolve(src.name))
            into(parent.projectDirectory.resolve("src/${src.name}"))
            dependsOn(prepareTask)
        }
    }
}