package dev.kikugie.stitcher.transform.replacement

import org.ahocorasick.interval.Interval
import org.ahocorasick.trie.PayloadTrie

internal interface ReplacementExecutor<T : Replacement> {
    fun replace(builder: StringBuilder)

    companion object {
        operator fun invoke(replacements: List<Replacement>, identifiers: Collection<String>): ReplacementExecutor<Replacement> =
            CompositeReplacementExecutor(replacements, identifiers)
    }
}

private class CompositeReplacementExecutor(replacements: List<Replacement>, identifiers: Collection<String>) : ReplacementExecutor<Replacement> {
    private val stringExecutor = StringReplacementExecutor(replacements, identifiers)
    private val regexExecutor = RegexReplacementExecutor(replacements, identifiers)

    override fun replace(builder: StringBuilder) {
        stringExecutor.replace(builder)
        regexExecutor.replace(builder)
    }
}

// TODO: Add case-insensitive and word matching options as Stonecutter flags
private class StringReplacementExecutor(replacements: List<Replacement>, identifiers: Collection<String>) : ReplacementExecutor<StringReplacement> {
    private val trie: PayloadTrie<ReplacementAction> by lazy {
        buildActions(buildReplacements(replacements, identifiers)).ignoreOverlaps().build()
    }

    override fun replace(builder: StringBuilder) {
        for (emit in trie.parseText(builder).reversed())
            emit.payload?.replace(builder, emit)
    }

    private fun buildReplacements(replacements: List<Replacement>, identifiers: Collection<String>): List<StringReplacement> {
        val stringReplacements = replacements.asSequence().filterIsInstance<StringReplacement>()
        val unnamedReplacements = stringReplacements.filter { it.identifier == null }.toList()
        val namedReplacements = stringReplacements.filter { it.identifier in identifiers }.toList()
        return when {
            namedReplacements.isEmpty() && unnamedReplacements.isEmpty() -> emptyList()
            namedReplacements.isEmpty() -> unnamedReplacements
            unnamedReplacements.isEmpty() -> namedReplacements
            else -> ReplacementBuilder.string(unnamedReplacements)
                // TODO: Handle the exception
                .apply { for (it in namedReplacements) add(it.copy(identifier = null)).getOrThrow() }
                .build()
        }
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

private class RegexReplacementExecutor(replacements: List<Replacement>, identifiers: Collection<String>) : ReplacementExecutor<RegexReplacement> {
    private val replacements: List<RegexReplacement> by lazy {
        @Suppress("UNCHECKED_CAST")
        replacements.filter { it is RegexReplacement && (it.identifier == null || it.identifier in identifiers) } as List<RegexReplacement>
    }

    override fun replace(builder: StringBuilder) {
        for (repl in replacements) for (match in repl.pattern.findAll(builder).toList().reversed())
            builder.replaceRange(match.range, repl.target)
    }
}