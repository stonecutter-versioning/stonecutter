package dev.kikugie.stonecutter.controller.file

import dev.kikugie.stitcher.issue.ProblemSource
import dev.kikugie.stitcher.parser.adapter.ScannerAdapter
import dev.kikugie.stitcher.transform.strategy.CommentingStrategy
import dev.kikugie.stitcher.transform.strategy.SwappingStrategy
import dev.kikugie.stitcher.transform.strategy.UncommentingStrategy
import org.antlr.v4.runtime.CharStream
import java.io.Serializable

@JvmRecord
internal data class ScannerModel(
    val openers: IntArray,
    val closers: IntArray,
    val lexer: ScannerBuilder.LexerFactory
): Serializable, ScannerAdapter.Factory {
    constructor(builder: ScannerBuilder) : this(
        builder.openers.get().toIntArray(),
        builder.closers.get().toIntArray(),
        builder.lexer.get()
    )

    override fun create(input: CharStream, problems: ProblemSource): ScannerAdapter =
        ScannerAdapter(lexer.create(input), openers, closers, problems)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ScannerModel) return false

        if (!openers.contentEquals(other.openers)) return false
        if (!closers.contentEquals(other.closers)) return false
        if (lexer != other.lexer) return false

        return true
    }

    override fun hashCode(): Int {
        var result = openers.contentHashCode()
        result = 31 * result + closers.contentHashCode()
        result = 31 * result + lexer.hashCode()
        return result
    }
}

@JvmRecord
internal data class HandlerModel(
    val scanner: ScannerModel,
    val commenter: CommentingStrategy,
    val uncommenter: UncommentingStrategy,
    val swapper: SwappingStrategy
) : Serializable {
    constructor(builder: HandlerBuilder) : this(
        ScannerModel(builder.scanner.get()),
        builder.commenter.get(),
        builder.uncommenter.get(),
        builder.swapper.get()
    )
}