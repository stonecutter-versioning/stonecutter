package dev.kikugie.stitcher.transform

import dev.kikugie.stitcher.issue.ProblemSource
import dev.kikugie.stitcher.transform.replacement.ReplacementProcessor
import org.antlr.v4.runtime.CharStream

internal class RuntimeState(val input: CharStream, val problems: ProblemSource, parameters: TransformParameters) {
    val replacer: ReplacementProcessor = ReplacementProcessor(parameters.replacements, problems)
}