package dev.kikugie.stitcher.antlr

import dev.kikugie.stitcher.issue.ProblemSink
import dev.kikugie.stitcher.issue.at
import dev.kikugie.stitcher.issue.problem
import dev.kikugie.stitcher.issue.report
import dev.kikugie.stitcher.util.FileLineIndex
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer

// TODO: Finish this shi
internal class InlineErrorListener(val sink: ProblemSink, val index: FileLineIndex, val offset: Int) : BaseErrorListener() {
    override fun syntaxError(recognizer: Recognizer<*, *>, symbol: Any?, line: Int, charPositionInLine: Int, msg: String?, e: RecognitionException?) {
        val globalOffset = index.indexOf(line, charPositionInLine) + offset
        sink.at(globalOffset) report problem { msg ?: e?.message ?: "IDK" }
    }
}