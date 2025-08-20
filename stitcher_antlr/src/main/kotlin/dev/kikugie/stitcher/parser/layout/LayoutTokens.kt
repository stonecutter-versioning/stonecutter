package dev.kikugie.stitcher.parser.layout

import org.antlr.v4.runtime.Vocabulary
import org.antlr.v4.runtime.VocabularyImpl

internal object LayoutTokens {
    const val CONTENT: Int = 1
    const val COMMENT_OPEN: Int = 2
    const val COMMENT_BODY: Int = 3
    const val COMMENT_CLOSE: Int = 4

    @JvmField val VOCABULARY: Vocabulary = VocabularyImpl(
        emptyArray(),
        arrayOf("CONTENT", "COMMENT_OPEN", "COMMENT_BODY", "COMMENT_CLOSE")
    )
}