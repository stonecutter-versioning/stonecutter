package dev.kikugie.stitcher.transform

import dev.kikugie.stitcher.issue.ProblemSink
import dev.kikugie.stitcher.issue.at
import dev.kikugie.stitcher.issue.problem
import dev.kikugie.stitcher.issue.report
import dev.kikugie.stitcher.transform.replacement.Replacement
import dev.kikugie.stitcher.transform.replacement.ReplacementExecutor
import org.antlr.v4.runtime.CharStream

internal data class RuntimeState(val input: CharStream, val sink: ProblemSink) {
    private var replacementExecutor: ReplacementExecutor<*>? = null
    private val enabledReplacementIds: MutableSet<String> = mutableSetOf()

    val replacer: ReplacementExecutor<*>? get() = replacementExecutor

    fun initializeReplacements(replacements: List<Replacement>) {
        if (replacementExecutor == null)
            replacementExecutor = SafeReplacementExecutor(replacements)
    }

    fun includeReplacement(identifier: String) {
        enabledReplacementIds += identifier
    }

    private inner class SafeReplacementExecutor(replacements: List<Replacement>) : ReplacementExecutor<Replacement> {
        val delegate = ReplacementExecutor(replacements, enabledReplacementIds)
        var failed = false

        override fun replace(builder: StringBuilder) = if (failed) Unit else try {
            delegate.replace(builder)
        } catch (e: Exception) {
            failed = true
            sink.at(-1, -1) report if (e.message != null) problem { e.message!! }
            else problem(e) { "Failed to perform replacements" }
        }
    }
}