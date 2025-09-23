package dev.kikugie.stitcher.transform

import dev.kikugie.stitcher.issue.ProblemSink
import dev.kikugie.stitcher.transform.replacement.Replacement
import dev.kikugie.stitcher.transform.replacement.ReplacementExecutor
import org.antlr.v4.runtime.CharStream

internal data class RuntimeState(val input: CharStream, val sink: ProblemSink) {
    private var replacementExecutor: ReplacementExecutor<*>? = null
    private val enabledReplacementIds: MutableSet<String> = mutableSetOf()

    val replacer: ReplacementExecutor<*>? get() = replacementExecutor

    fun initializeReplacements(replacements: List<Replacement>) {
        if (replacementExecutor == null)
            replacementExecutor = ReplacementExecutor(replacements, enabledReplacementIds)
    }

    fun includeReplacement(identifier: String) {
        enabledReplacementIds += identifier
    }
}