package dev.kikugie.stitcher.antlr

import dev.kikugie.stitcher.issue.ProblemSource
import dev.kikugie.stitcher.util.FileLineIndex
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer

// TODO: Finish this shi
internal class InlineErrorListener(val problems: ProblemSource, val index: FileLineIndex, val offset: Int = 0) : BaseErrorListener(), ProblemSource by problems {
    override fun syntaxError(recognizer: Recognizer<*, *>, symbol: Any?, line: Int, charPositionInLine: Int, msg: String?, e: RecognitionException?) {
        val globalOffset = index.indexOf(line, charPositionInLine) + offset
        at(globalOffset) report (msg ?: e?.message ?: "IDK")
    }
}