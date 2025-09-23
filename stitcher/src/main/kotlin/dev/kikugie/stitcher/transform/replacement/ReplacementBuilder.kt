package dev.kikugie.stitcher.transform.replacement

private fun pair(a: String, b: String): String = "({'$a'} -> '$b')"

public interface ReplacementBuilder<T : Replacement> {
    public fun add(replacement: T): Result<Unit>
    public fun build(): List<T>

    public companion object {
        public operator fun invoke(): ReplacementBuilder<Replacement> = CompositeReplacementBuilder()
        internal fun string(repl: List<StringReplacement>): ReplacementBuilder<StringReplacement> =
            StringReplacementBuilder(repl)
    }
}

private class CompositeReplacementBuilder : ReplacementBuilder<Replacement> {
    private val identifiers: MutableSet<String> = mutableSetOf()
    private val regexBuilder: RegexReplacementBuilder = RegexReplacementBuilder(identifiers)
    private val stringBuilder: StringReplacementBuilder = StringReplacementBuilder(identifiers)

    override fun add(replacement: Replacement): Result<Unit> = when (replacement) {
        is StringReplacement -> stringBuilder.add(replacement)
        is RegexReplacement -> regexBuilder.add(replacement)
    }

    override fun build(): List<Replacement> = stringBuilder.build() + regexBuilder.build()
}

private class StringReplacementBuilder(val identifiers: MutableSet<String>) : ReplacementBuilder<StringReplacement> {
    constructor(list: List<StringReplacement>) : this(mutableSetOf()) {
        for (it in list) stubs += StringReplacementStub(it.target, it.sources.toMutableSet(), it.identifier)
    }
    private val stubs: MutableList<StringReplacementStub> = mutableListOf()

    override fun build(): List<StringReplacement> = stubs.map(StringReplacementStub::build)

    override fun add(replacement: StringReplacement): Result<Unit> = kotlin.runCatching {
        require(replacement.sources.isNotEmpty()) { "Empty replacement sources" }
        for (source in replacement.sources) merge(replacement.target, source, replacement.identifier)
    }

    private fun merge(target: String, source: String, identifier: String?) {
        // TODO: Check ID validity
        require(source.isNotEmpty() && target.isNotEmpty()) { "Cannot perform replacement with empty values ${pair(source, target)}" }

        if (identifier == null) {
            if (stubs.none { it.tryMerge(target, source) }) stubs += StringReplacementStub(target, source)
            return
        }

        val match: StringReplacementStub? = stubs.find { it.identifier == identifier }
        require(match != null || identifier !in identifiers) { "A regex replacement is already registered for id '$identifier'" }

        if (match == null) stubs += StringReplacementStub(target, source, identifier)
        else require(match.tryMerge(target, source)) { "Unable to merge $match + ${pair(source, target)}" }
        identifiers += identifier
    }

    private fun StringReplacementStub.tryMerge(newTarget: String, newSource: String): Boolean = when {
        // Transitive replacement: ({a, b} -> c) + ({c} -> d) = ({a, b, c} -> d)
        newSource == target -> {
            require(newTarget !in sources) { "Replacing ${pair(newSource, newTarget)} forms a cycle with $this" }
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
            require(newSource !in sources) { "Ambiguous replacement ${pair(newSource, newTarget)} + $this" }
            false
        }
    }

    private data class StringReplacementStub(var target: String, val sources: MutableSet<String>, val identifier: String?) {
        constructor(target: String, source: String, identifier: String? = null) : this(target, mutableSetOf(source), identifier)

        fun build(): StringReplacement = StringReplacement(target, sources, identifier)
        override fun toString(): String = "str${if (identifier != null) "@$identifier" else ""}({${sources.joinToString { "'$it'" }}} -> '$target')"
    }
}

private class RegexReplacementBuilder(val identifiers: MutableSet<String>) : ReplacementBuilder<RegexReplacement> {
    private val replacements: MutableList<RegexReplacement> = mutableListOf()

    override fun build(): List<RegexReplacement> = replacements.toList()

    override fun add(replacement: RegexReplacement): Result<Unit> = replacement.runCatching {
        // TODO: Check ID validity
        require(pattern.pattern.isNotEmpty() && target.isNotEmpty()) { "Cannot perform replacement with empty values ${pair(pattern.pattern, target)}" }
        require(identifier == null || identifier !in identifiers) { "A replacement is already registered for id '$identifier'" }

        replacements += replacement
        if (identifier != null) identifiers += identifier
    }
}