package dev.kikugie.stitcher.transform.replacement

import org.ahocorasick.interval.Interval
import org.ahocorasick.trie.PayloadTrie

/**
 * Mutably applies replacements to [StringBuilder] contents.
 */
internal fun interface ReplacementExecutor {
    fun replace(builder: StringBuilder)

    companion object {
        operator fun invoke(replacements: List<Replacement>): ReplacementExecutor =
            if (replacements.isEmpty()) DummyReplacementExecutor else CompositeReplacementExecutor(replacements)
    }
}

private object DummyReplacementExecutor : ReplacementExecutor {
    override fun replace(builder: StringBuilder) = Unit
}

private class CompositeReplacementExecutor(replacements: List<Replacement>) : ReplacementExecutor {
    private val stringExecutor = StringReplacementExecutor(replacements.filterIsInstance<StringReplacement>())
    private val regexExecutor = RegexReplacementExecutor(replacements.filterIsInstance<RegexReplacement>())

    override fun replace(builder: StringBuilder) {
        stringExecutor.replace(builder)
        regexExecutor.replace(builder)
    }
}

// TODO: Add case-insensitive and word matching options as Stonecutter flags
private class StringReplacementExecutor(replacements: List<StringReplacement>) : ReplacementExecutor {
    private val trie: PayloadTrie<ReplacementAction> by lazy { buildActions(replacements).ignoreOverlaps().build() }

    override fun replace(builder: StringBuilder) {
        for (emit in trie.parseText(builder).reversed())
            emit.payload?.replace(builder, emit)
    }

    private fun buildActions(replacements: List<StringReplacement>) = PayloadTrie.builder<ReplacementAction>().apply {
        for (repl in replacements) {
            addKeyword(repl.target)
            for (src in repl.sources)
                addKeyword(src, ReplacementAction(repl.target))
        }
    }

    private class ReplacementAction(val value: String) {
        fun replace(builder: StringBuilder, emit: Interval) {
            builder.replace(emit.start, emit.end + 1, value)
        }
    }
}

private class RegexReplacementExecutor(val replacements: List<RegexReplacement>) : ReplacementExecutor {
    override fun replace(builder: StringBuilder) {
        for (repl in replacements) for (match in repl.regex.findAll(builder).toList().reversed())
            builder.replaceRange(match.range, repl.target)
    }
}