package dev.kikugie.stitcher.transform

import dev.kikugie.stitcher.issue.ProblemSink
import org.antlr.v4.runtime.CharStream

internal data class RuntimeParameters(val input: CharStream, val sink: ProblemSink) {
}