package dev.kikugie.stonecutter.build.util

import dev.kikugie.stonecutter.build.task.StonecutterPatternFilterable
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet

internal class RestrictedPatternFilterable : StonecutterPatternFilterable, PatternFilterable by PatternSet()