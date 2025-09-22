package dev.kikugie.stitcher.parse.inline

import dev.kikugie.stitcher.issue.ProblemSink
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer

internal class InlineErrorListener(val sink: ProblemSink, val offset: Int) : BaseErrorListener() {
    override fun syntaxError(recognizer: Recognizer<*, *>, symbol: Any?, line: Int, charPositionInLine: Int, msg: String?, e: RecognitionException?) {
    }
}