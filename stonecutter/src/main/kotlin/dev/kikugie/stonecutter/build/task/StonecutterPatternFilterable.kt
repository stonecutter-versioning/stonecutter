package dev.kikugie.stonecutter.build.task

import groovy.lang.Closure
import org.gradle.api.file.FileTreeElement
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.util.PatternFilterable

private const val INCLUDES_MESSAGE: String =
    "Files included in Stonecutter processing should be defined using file handlers. See https://stonecutter.kikugie.dev/wiki/config/handlers"

public interface StonecutterPatternFilterable : PatternFilterable {
    @Deprecated(INCLUDES_MESSAGE)
    override fun include(vararg includes: String): PatternFilterable

    @Deprecated(INCLUDES_MESSAGE)
    override fun include(includes: Iterable<String>): PatternFilterable

    @Deprecated(INCLUDES_MESSAGE)
    override fun include(includeSpec: Spec<FileTreeElement>): PatternFilterable

    @Deprecated(INCLUDES_MESSAGE)
    override fun include(includeSpec: Closure<*>): PatternFilterable
}