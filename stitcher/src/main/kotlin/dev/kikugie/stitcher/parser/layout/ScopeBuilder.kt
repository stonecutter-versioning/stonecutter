package dev.kikugie.stitcher.parser.layout

import dev.kikugie.commons.text.countMatching
import dev.kikugie.commons.text.countWhile
import dev.kikugie.commons.then
import dev.kikugie.stitcher.data.composite.*
import dev.kikugie.stitcher.data.composite.DefinitionToken.Type.*
import dev.kikugie.stitcher.data.custom.ClosedScope
import dev.kikugie.stitcher.data.custom.WordScope
import dev.kikugie.stitcher.data.leaf.LeafType
import dev.kikugie.stitcher.issue.ProblemSink
import dev.kikugie.stitcher.parser.StitcherTokenFactory
import dev.kikugie.stitcher.util.AntlrToken
import dev.kikugie.stitcher.util.WHITESPACES
import org.antlr.v4.runtime.TokenSource
import org.antlr.v4.runtime.misc.Pair
import kotlin.math.max

internal sealed interface ScopeBuilder {
    fun build(factory: StitcherTokenFactory): BlockToken

    interface ContainerScopeBuilder : ScopeBuilder {
        fun tryAccept(block: ScopeBuilder, source: TokenSource): AcceptResult
    }

    class Root(
        private val entries: MutableList<ScopeBuilder> = mutableListOf()
    ) : ContainerScopeBuilder {
        override fun build(factory: StitcherTokenFactory): RootBlock =
            RootBlock(entries.map { it.build(factory) })

        override fun tryAccept(block: ScopeBuilder, source: TokenSource): AcceptResult =
            entries merge block then AcceptResult.ConsumedOpen
    }

    class Code(
        private val host: Comment,
        val marker: AntlrToken,
        val definition: DefinitionToken,
        private val entries: MutableList<ScopeBuilder> = mutableListOf()
    ) : ContainerScopeBuilder {
        val kind: Int get() = marker.type
        val type: DefinitionToken.Type get() = definition.type
        var satisfied: Boolean = type.isScoped
            private set

        override fun build(factory: StitcherTokenFactory): CodeBlock = CodeBlock(
            host.build(factory), factory.fromAntlrToken(marker), definition, entries.map { it.build(factory) }
        )

        override fun tryAccept(block: ScopeBuilder, source: TokenSource): AcceptResult = when (type) {
            SCOPED_OPENER, SCOPED_EXTENSION -> entries merge block then AcceptResult.ConsumedOpen
            LINE_OPENER, LINE_EXTENSION -> if (satisfied) AcceptResult.Rejected else acceptLine(block, source)
            WORD_OPENER, WORD_EXTENSION -> if (satisfied) AcceptResult.Rejected else acceptWord(block, source)
            else -> AcceptResult.Rejected
        }

        internal fun checkUnfinished(sink: ProblemSink, closed: Boolean): Unit = when (val opener = definition.opener) {
            is ClosedScope -> if (closed) Unit else with(sink) {
                at(definition.opener!!) report problem("Unclosed scope")
            }
            is WordScope -> if (opener.literal == null || satisfied) Unit else with(sink) {
                at(opener.literal) report problem("Failed to find the matching string")
            }
            null -> Unit
        }

        private fun acceptLine(block: ScopeBuilder, source: TokenSource): AcceptResult =
            if (block !is Content) consumeFinal(block)
            else handleSplit(block, source, block.builder.consumeLine())

        private fun acceptWord(block: ScopeBuilder, source: TokenSource): AcceptResult {
            if (block !is Content) return consumeFinal(block)

            val opener = definition.opener as WordScope
            val split = if (opener.literal == null) block.builder.consumeWordDefault()
            else block.builder.consumeWordCustom(opener.expectedStr, opener.isCapturing)
            return handleSplit(block, source, split)
        }

        private fun consumeFinal(block: ScopeBuilder): AcceptResult {
            entries merge block
            satisfied = true
            return AcceptResult.ConsumedFinal
        }

        private fun handleSplit(block: Content, source: TokenSource, split: Int): AcceptResult {
            // The entire block was consumed, but we don't have the match yet
            if (split == -1) {
                entries merge block
                return AcceptResult.ConsumedOpen
            }

            // The entire block matches
            if (split == block.builder.length)
                return consumeFinal(block)

            satisfied = true
            entries merge source.createContent(block.start, block.start + split - 1, block.builder.take(split))
                .let(::Content)
            return source.createContent(block.start + split, block.stop, block.builder.substring(split))
                .let { AcceptResult.ConsumedPartial(Content(it)) }
        }
    }

    class Content(val start: Int, var stop: Int, val builder: StringBuilder) : ScopeBuilder {
        constructor(token: AntlrToken) : this(token.startIndex, token.stopIndex, StringBuilder(token.text))

        var isBlank: Boolean = builder.isBlank()
            private set

        override fun build(factory: StitcherTokenFactory): ContentBlock {
            val leaf = factory.create(LeafType(LayoutParser.CONTENT), start..stop, builder.toString())
            return ContentBlock(leaf, isBlank)
        }

        operator fun plusAssign(token: AntlrToken) {
            isBlank = isBlank && token.text.isBlank()
            stop = max(stop, token.stopIndex)
            builder.append(token.text)
        }

        operator fun plusAssign(block: Content) {
            isBlank = isBlank && block.isBlank
            stop = max(stop, block.stop)
            builder.append(block.builder)
        }
    }

    class Comment private constructor(private val ranges: IntArray, private val strings: Array<String>) : ScopeBuilder {
        constructor(opener: AntlrToken, body: AntlrToken, closer: AntlrToken) : this(
            intArrayOf(opener.startIndex, opener.stopIndex, body.startIndex, body.stopIndex, closer.startIndex, closer.stopIndex),
            arrayOf(opener.text, body.text, closer.text)
        )

        override fun build(factory: StitcherTokenFactory): CommentBlock = CommentBlock(
            factory.create(LeafType(LayoutParser.COMMENT_OPEN), ranges[0]..ranges[1], strings[0]),
            factory.create(LeafType(LayoutParser.COMMENT_BODY), ranges[2]..ranges[3], strings[1]),
            factory.create(LeafType(LayoutParser.COMMENT_CLOSE), ranges[4]..ranges[5], strings[2]),
        )
    }
}

internal sealed interface AcceptResult {
    data object Rejected : AcceptResult
    data object ConsumedOpen : AcceptResult
    data object ConsumedFinal : AcceptResult
    data class ConsumedPartial(val remaining: ScopeBuilder.Content) : AcceptResult
}

private fun CharSequence.consumeLine(): Int {
    // Ignore all whitespaces and line breaks preceding the content
    var index = countMatching(*WHITESPACES)
    if (index == length) return -1

    // Cancerous way to consume the line with the line break
    var state = 0
    index += countWhile(index) {
        when (it) {
            '\n' if state == 0 -> true.also { state = 1 }
            '\r' if state in 0..1 -> true.also { state = 2 }
            else -> state == 0
        }
    }

    // -1 indicates we didn't reach a newline
    return if (state != 0) index else -1
}

private fun CharSequence.consumeWordDefault(): Int {
    var index = countMatching(*WHITESPACES)
    index += countWhile(index) { it !in WHITESPACES }
    return index
}

private fun CharSequence.consumeWordCustom(match: String, capturing: Boolean): Int {
    val index = indexOf(match)
    return when {
        index < 0 -> -1
        capturing -> index + match.length
        else -> index
    }
}

private fun TokenSource.createContent(start: Int, stop: Int, text: CharSequence) = tokenFactory
    .create(Pair(this, inputStream), LayoutParser.CONTENT, text.toString(), AntlrToken.DEFAULT_CHANNEL, start, stop, -1, -1)

private infix fun MutableList<ScopeBuilder>.merge(entry: ScopeBuilder) = when (val it = lastOrNull()) {
    is ScopeBuilder.Content if (entry is ScopeBuilder.Content) -> it += entry
    else -> this += entry
}