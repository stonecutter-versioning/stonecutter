package dev.kikugie.stitcher.parser.layout

import dev.kikugie.stitcher.data.BlockToken
import dev.kikugie.stitcher.data.DefinitionToken
import dev.kikugie.stitcher.data.LeafToken
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.Token

internal sealed interface ScopeBuilder {
    val parent: ScopeBuilder?
    val entries: MutableList<ScopeBuilder>

    fun build(): BlockToken

    fun code(marker: LeafToken, definition: DefinitionToken) = Code(this, marker, definition).also {
        entries += it
    }
    fun content(token: Token) {
        entries += Entry(this, BlockToken.Content(token))
    }
    fun content(range: IntRange, source: CharStream) {
        entries += Entry(this, BlockToken.Content(range, source))
    }
    fun comment(opener: Token, body: Token, closer: Token) {
        entries += Entry(this, BlockToken.Comment(opener, body, closer))
    }

    class Root : ScopeBuilder {
        override val parent: ScopeBuilder? get() = null
        override val entries: MutableList<ScopeBuilder> = mutableListOf()
        override fun build(): BlockToken = BlockToken.Root(entries.map(ScopeBuilder::build))
    }

    class Code(override val parent: ScopeBuilder, val marker: LeafToken, val definition: DefinitionToken) : ScopeBuilder {
        override val entries: MutableList<ScopeBuilder> = mutableListOf()
        override fun build(): BlockToken = BlockToken.Code(marker, definition, entries.map(ScopeBuilder::build))
    }

    class Entry(override val parent: ScopeBuilder, val block: BlockToken) : ScopeBuilder {
        override val entries: MutableList<ScopeBuilder> get() = emptyList<ScopeBuilder>() as MutableList<ScopeBuilder>
        override fun build(): BlockToken = block
    }
}