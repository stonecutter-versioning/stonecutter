package dev.kikugie.stonecutter.build.task

import groovy.lang.Closure
import org.gradle.api.file.FileTreeElement
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.util.PatternSet

private const val INCLUDES_MESSAGE: String =
    "Files included in Stonecutter processing should be defined using file handlers. See https://stonecutter.kikugie.dev/wiki/config/handlers"

public class StonecutterPatternFilterable : PatternSet() {
    @Deprecated(INCLUDES_MESSAGE)
    override fun include(vararg includes: String): PatternSet = super.include(*includes)

    @Deprecated(INCLUDES_MESSAGE)
    override fun include(includes: Iterable<Any?>): PatternSet = super.include(includes)

    @Deprecated(INCLUDES_MESSAGE)
    override fun include(includeSpec: Spec<FileTreeElement>): PatternSet = super.include(includeSpec)

    @Deprecated(INCLUDES_MESSAGE)
    override fun include(includeSpec: Closure<*>): PatternSet = super.include(includeSpec)
}