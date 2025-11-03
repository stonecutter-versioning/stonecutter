package dev.kikugie.stitcher.antlr

import dev.kikugie.stitcher.issue.ProblemSink
import dev.kikugie.stitcher.issue.ProblemSource
import dev.kikugie.stitcher.util.FileLineIndex
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer

// TODO: Finish this shi
internal class InlineErrorListener(val sink: ProblemSink, val offset: Int = 0, override val index: FileLineIndex = sink.index) : BaseErrorListener(), ProblemSource by sink {
    override fun syntaxError(recognizer: Recognizer<*, *>, symbol: Any?, line: Int, charPositionInLine: Int, msg: String?, e: RecognitionException?) {
        val globalOffset = index.indexOf(line, charPositionInLine) + offset
        at(globalOffset) report (msg ?: e?.message ?: "IDK")
    }
}