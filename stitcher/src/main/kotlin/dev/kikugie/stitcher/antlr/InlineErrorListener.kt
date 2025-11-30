package dev.kikugie.stitcher.antlr

import dev.kikugie.stitcher.issue.ProblemLocation
import dev.kikugie.stitcher.issue.ProblemSource
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer

// TODO: Finish this shi
internal class InlineErrorListener(val problems: ProblemSource, val offset: ProblemLocation) : BaseErrorListener(), ProblemSource by problems {
    override fun syntaxError(recognizer: Recognizer<*, *>, symbol: Any?, line: Int, charPositionInLine: Int, msg: String?, e: RecognitionException?) {
        val location = ProblemLocation(line, charPositionInLine + 1)
        offset.resolve(location) report (msg ?: e?.message ?: "IDK")
    }
}