package dev.kikugie.stonecutter.build

import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.build.param.StonecutterBuildProperties
import dev.kikugie.stonecutter.build.task.StonecutterBuildTasks.Companion.tasksContainer
import dev.kikugie.stonecutter.build.util.flags
import dev.kikugie.stonecutter.build.util.tasks
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
import dev.kikugie.semver.data.Version as ParsedVersion

@OptIn(StonecutterInternalAPI::class)
internal abstract class StonecutterBuildImpl(val project: Project)
    : StonecutterBuildExtension, VersionOperations<ParsedVersion> by LenientOperations {
    override val node: ProjectNode =
        checkNotNull(project.gradle.getContainer<ProjectNodeContainer>()[project]) { "${project.hierarchy} is not a registered Stonecutter node" }

    override val filters: PatternFilterable
        get() = properties.filters

    private val properties: StonecutterBuildProperties =
        project.gradle.getContainer<BuildPropertiesContainer>()[node]

    init {
        for (schema in properties.extensions.extensionsSchema)
            extensions.add(schema.name, properties.extensions.getByName(schema.name))
        extensions.tasksContainer("tasks", this)
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
        val overrides = project.projectDirectory.resolve("src/${src.name}")
        val prepareTask = tasks.registerPrepareTask(src) {
            params.set(properties.params)
            project.parent!!.file("src/${src.name}").let(root::set)
            project.provider { project.parent!!.fileTree("src/${src.name}").matching(filters) }.let { source.setFrom(it) }
            tasks.processedCacheDir.resolve(src.name).let(destination::set)
        }

        tasks.registerGenerateTask(src) {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
            from(project.parent!!.projectDirectory.resolve("src/${src.name}"), tasks.processedCacheDir.resolve(src.name))
            exclude { !it.isDirectory && it.relativePath.getFile(overrides).exists() }
            into(tasks.generatedSourcesDir.resolve(src.name))
            dependsOn(prepareTask)
        }

        tasks.registerMergeTask(src) {
            from(tasks.processedCacheDir.resolve(src.name))
            into(project.parent!!.projectDirectory.resolve("src/${src.name}"))
            dependsOn(prepareTask)
        }
    }
}