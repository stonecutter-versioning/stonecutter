package dev.kikugie.stitcher.transform.replacement

import dev.kikugie.stitcher.util.isValidIdentifier

public interface ReplacementBuilder<T : Replacement> {
    public fun add(replacement: T): Result<Unit>
    public fun build(): List<T>

    public companion object {
        public operator fun invoke(entries: Iterable<Replacement> = emptyList()): ReplacementBuilder<Replacement> =
            CompositeReplacementBuilder(entries)

        public fun string(entries: Iterable<StringReplacement> = emptyList()) : ReplacementBuilder<StringReplacement> =
            StringReplacementBuilder(entries)

        public fun regex(entries: Iterable<RegexReplacement> = emptyList()) : ReplacementBuilder<RegexReplacement> =
            RegexReplacementBuilder(entries)
    }
}

private class CompositeReplacementBuilder(entries: Iterable<Replacement>) : ReplacementBuilder<Replacement> {
    private val regexBuilder: RegexReplacementBuilder = RegexReplacementBuilder(entries.filterIsInstance<RegexReplacement>())
    private val stringBuilder: StringReplacementBuilder = StringReplacementBuilder(entries.filterIsInstance<StringReplacement>())

    override fun add(replacement: Replacement): Result<Unit> = when (replacement) {
        is StringReplacement -> stringBuilder.add(replacement)
        is RegexReplacement -> regexBuilder.add(replacement)
    }

    override fun build(): List<Replacement> = stringBuilder.build() + regexBuilder.build()
}

private class StringReplacementBuilder() : ReplacementBuilder<StringReplacement> {
    constructor(entries: Iterable<StringReplacement>) : this() {
        for (it in entries) stubs += StringReplacementStub(it.target, it.sources.toMutableSet(), it.identifier)
    }
    private val stubs: MutableList<StringReplacementStub> = mutableListOf()

    override fun build(): List<StringReplacement> = stubs.map(StringReplacementStub::build)

    override fun add(replacement: StringReplacement): Result<Unit> = replacement.runCatching {
        if (target.isEmpty() || sources.any(String::isEmpty))
            throw ReplacementException("Empty replacement value", toDescriptorString())
        if (identifier != null && !identifier.isValidIdentifier())
            throw ReplacementException("Invalid replacement identifier", toDescriptorString())
        for (source in sources) merge(target, source, identifier)
    }

    private fun merge(target: String, source: String, identifier: String?) {
        val matching = stubs.asSequence().filter { it.identifier == identifier }
        if (matching.none { it.tryMerge(target, source) }) stubs += StringReplacementStub(target, source, identifier)
    }

    private fun StringReplacementStub.tryMerge(newTarget: String, newSource: String): Boolean = when {
        // Transitive replacement: ({a, b} -> c) + ({c} -> d) = ({a, b, c} -> d)
        newSource == target -> {
            if (newTarget in sources)
                throw ReplacementException("Replacements forms a cycle", "${pair(newSource, newTarget)} + $this")
            sources += newSource
            target = newTarget
            true
        }

        // Composite replacement: ({a, b} -> d) + ({c} -> d) = ({a, b, c} -> d)
        // Transitive replacement: ({a, c} -> d) + ({b} -> c) = ({a, b, c} -> d)
        newTarget == target || newTarget in sources -> {
            sources += newSource
            true
        }

        // Ambiguous replacement: ({a, b} -> d) + ({a, c} -> d)
        else -> {
            if (newSource in sources)
                throw ReplacementException("Ambiguous replacement result", "${pair(newSource, newTarget)} + $this")
            false
        }
    }

    private fun StringReplacement.toDescriptorString(): String =
        "string${if (identifier != null) "@$identifier" else ""}({${sources.joinToString { "'$it'" }}} -> '$target')"

    private fun pair(a: String, b: String): String = "({'$a'} -> '$b')"

    private data class StringReplacementStub(var target: String, val sources: MutableSet<String>, val identifier: String?) {
        constructor(target: String, source: String, identifier: String? = null) : this(target, mutableSetOf(source), identifier)

        fun build(): StringReplacement = StringReplacement(target, sources, identifier)
        override fun toString(): String =
            "string${if (identifier != null) "@$identifier" else ""}({${sources.joinToString { "'$it'" }}} -> '$target')"
    }
}

private class RegexReplacementBuilder() : ReplacementBuilder<RegexReplacement> {
    constructor(entries: Iterable<RegexReplacement>) : this() {
        replacements.addAll(entries)
    }
    private val replacements: MutableList<RegexReplacement> = mutableListOf()

    override fun build(): List<RegexReplacement> = replacements.toList()

    override fun add(replacement: RegexReplacement): Result<Unit> = replacement.runCatching {
        if (pattern.isEmpty() || target.isEmpty())
            throw ReplacementException("Empty replacement value", toDescriptorString())
        if (identifier != null && !identifier.isValidIdentifier())
            throw ReplacementException("Invalid replacement identifier", toDescriptorString())
        replacements += this
    }

    private fun RegexReplacement.toDescriptorString(): String =
        "regex${if (identifier != null) "@$identifier" else ""}({${pattern}} -> '$target')"
}