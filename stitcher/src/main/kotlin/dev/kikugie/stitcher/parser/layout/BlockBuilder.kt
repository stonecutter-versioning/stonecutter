package dev.kikugie.stitcher.parser.layout

import dev.kikugie.commons.ranges.extend
import dev.kikugie.commons.text.countMatching
import dev.kikugie.commons.text.countWhile
import dev.kikugie.commons.then
import dev.kikugie.stitcher.data.composite.*
import dev.kikugie.stitcher.data.composite.DefinitionToken.Type.*
import dev.kikugie.stitcher.data.custom.ClosedScope
import dev.kikugie.stitcher.data.custom.WordScope
import dev.kikugie.stitcher.data.leaf.LeafType
import dev.kikugie.stitcher.issue.ProblemSource
import dev.kikugie.stitcher.issue.at
import dev.kikugie.stitcher.parser.StitcherTokenFactory
import dev.kikugie.stitcher.util.AntlrToken
import dev.kikugie.stitcher.util.WHITESPACES
import dev.kikugie.stitcher.util.hasLineBreak
import dev.kikugie.stitcher.util.merge
import dev.kikugie.stitcher.util.range

/**
 * Mutable version of [BlockToken] used in [LayoutParser].
 */
internal sealed interface BlockBuilder {
    val factory: StitcherTokenFactory

    fun build(): BlockToken

    sealed interface Scoped : BlockBuilder {
        fun accept(block: BlockBuilder): BlockAcceptResult
        fun finalize(problems: ProblemSource, isEOF: Boolean): Unit = Unit
    }
}

internal class ContentBuilder(
    override val factory: StitcherTokenFactory,
    val range: IntRange,
    val value: String
) : BlockBuilder {
    constructor(factory: StitcherTokenFactory, content: AntlrToken)
        : this(factory, content.range, content.text)

    override fun build(): ContentBlock = ContentBlock(
        factory.create(LeafType(LayoutParser.CONTENT), range, value), value.isBlank()
    )
}

internal class CommentBuilder(
    override val factory: StitcherTokenFactory,
    val range: IntRange,
    val value: String,
    val opener: AntlrToken?,
    val closer: AntlrToken?,
) : BlockBuilder {
    constructor(factory: StitcherTokenFactory, opener: AntlrToken, body: AntlrToken, closer: AntlrToken)
        : this(factory, body.range, body.text, opener, closer)

    override fun build(): CommentBlock = CommentBlock(
        opener?.let(factory::fromAntlrToken), factory.create(LeafType(LayoutParser.CONTENT), range, value), closer?.let(factory::fromAntlrToken)
    )
}

internal class RootBuilder(
    override val factory: StitcherTokenFactory,
    private val entries: MutableList<BlockBuilder> = mutableListOf()
) : BlockBuilder.Scoped {
    override fun build(): RootBlock = RootBlock(
        entries.map(BlockBuilder::build)
    )

    override fun accept(block: BlockBuilder): BlockAcceptResult =
        entries.addMerging(block) then BlockAcceptResult.ConsumedOpen
}

internal class CodeBuilder(
    override val factory: StitcherTokenFactory,
    val source: CommentBuilder,
    val marker: AntlrToken,
    val definition: DefinitionToken,
    val entries: MutableList<BlockBuilder> = mutableListOf()
) : BlockBuilder.Scoped {
    val kind: Int get() = marker.type
    val type: DefinitionToken.Type get() = definition.type
    var satisfied: Boolean = definition.type.isScoped
        private set

    override fun build(): CodeBlock = CodeBlock(
        source.build(), factory.fromAntlrToken(marker), definition, entries.map(BlockBuilder::build)
    )

    override fun accept(block: BlockBuilder): BlockAcceptResult = when (definition.type) {
        SCOPED_OPENER, SCOPED_EXTENSION -> entries.addMerging(block) then BlockAcceptResult.ConsumedOpen
        LINE_OPENER, LINE_EXTENSION -> if (satisfied) BlockAcceptResult.Rejected else acceptLine(block)
        WORD_OPENER, WORD_EXTENSION -> if (satisfied) BlockAcceptResult.Rejected else acceptWord(block)
        else -> BlockAcceptResult.Rejected
    }

    override fun finalize(problems: ProblemSource, isEOF: Boolean) = when (val opener = definition.opener) {
        is ClosedScope if isEOF -> with(problems) {
            at(opener) report "Unclosed scope"
        }

        is WordScope if opener.literal != null && !satisfied -> with(problems) {
            at(opener.literal) report "Failed to find the matching string"
        }

        else -> Unit
    }

    private fun acceptLine(block: BlockBuilder): BlockAcceptResult = when (block) {
        is ContentBuilder -> consumeContentSplit(block, consumeLine(block.value))
        is CommentBuilder -> consumeCommentSplit(block, consumeLine(block.value)).let {
            if (it !is BlockAcceptResult.ConsumedOpen) it
            else if (!block.value.isNotBlank() || !block.closer?.text.orEmpty().hasLineBreak()) it
            else BlockAcceptResult.ConsumedFinal
        }

        else -> consumeFinal(block)
    }

    private fun acceptWord(block: BlockBuilder): BlockAcceptResult =
        if (block is BlockBuilder.Scoped) consumeFinal(block)
        else acceptWord(block, definition.opener as WordScope)

    private fun acceptWord(block: BlockBuilder, scope: WordScope): BlockAcceptResult = when (block) {
        is ContentBuilder -> consumeContentSplit(
            block,
            if (scope.literal == null) consumeWordDefault(block.value)
            else consumeWordCustom(block.value, scope.expectedStr, scope.isCapturing)
        )

        is CommentBuilder -> consumeCommentSplit(
            block,
            if (scope.literal == null) consumeWordDefault(block.value)
            else consumeWordCustom(block.value, scope.expectedStr, scope.isCapturing)
        )

        else -> error("Unreachable")
    }

    private fun consumeFinal(block: BlockBuilder): BlockAcceptResult {
        satisfied = true
        return entries.addMerging(block) then BlockAcceptResult.ConsumedFinal
    }

    private fun consumeContentSplit(block: ContentBuilder, split: Int): BlockAcceptResult = when {
        // No end sequence was found
        split < 0 -> entries.addMerging(block) then BlockAcceptResult.ConsumedOpen

        // The entire content was consumed
        split == block.value.length -> consumeFinal(block)

        else -> {
            satisfied = true
            // Block before the split goes to this scope
            ContentBuilder(factory, block.range.first extend split, block.value.take(split))
                .let(entries::addMerging)
            // Return the remainder
            ContentBuilder(factory, block.range.first + split..block.range.last, block.value.substring(split))
                .let(BlockAcceptResult::ConsumedPartial)
        }
    }

    private fun consumeCommentSplit(block: CommentBuilder, split: Int): BlockAcceptResult = when {
        // No end sequence was found
        split < 0 -> entries.add(block) then BlockAcceptResult.ConsumedOpen

        // The entire comment was consumed
        split == block.value.length -> consumeFinal(block)

        else -> {
            satisfied = true
            // Split comment without a closer goes to this scope
            CommentBuilder(factory, block.range.first extend split, block.value.take(split), block.opener, null)
                .let(entries::add)
            // Return the remainder comment without an opener
            CommentBuilder(factory, block.range.first + split..block.range.last, block.value.substring(split), null, block.closer)
                .let(BlockAcceptResult::ConsumedPartial)
        }
    }
}

private fun MutableList<BlockBuilder>.addMerging(block: BlockBuilder) = when (val it = lastOrNull()) {
    is ContentBuilder if (block is ContentBuilder) ->
        this[lastIndex] = ContentBuilder(it.factory, merge(it.range, block.range), it.value + block.value)

    else -> this += block
}

private fun consumeLine(str: String): Int {
    // Ignore all whitespaces and line breaks preceding the content
    var index = str.countMatching(*WHITESPACES)
    if (index == str.length) return -1

    // Cancerous way to consume the line with the line break
    var state = 0
    index += str.countWhile(index) {
        when (it) {
            '\n' if state == 0 -> true.also { state = 1 }
            '\r' if state in 0..1 -> true.also { state = 2 }
            else -> state == 0
        }
    }

    // -1 indicates we didn't reach a newline
    return if (state != 0) index else -1
}

private fun consumeWordDefault(str: String): Int {
    var index = str.countMatching(*WHITESPACES)
    index += str.countWhile(index) { it !in WHITESPACES }
    return index
}

private fun consumeWordCustom(str: String, match: String, capturing: Boolean): Int {
    val index = str.indexOf(match)
    return when {
        index < 0 -> -1
        capturing -> index + match.length
        else -> index
    }
}