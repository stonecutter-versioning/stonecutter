package dev.kikugie.stitcher.data.replacement

import dev.kikugie.stitcher.data.token.ContentType
import dev.kikugie.stitcher.scanner.CommentRecognizer
import dev.kikugie.stitcher.scanner.Scanner
import dev.kikugie.stitcher.transformer.getOrSpace
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.collections.iterator

/**Represents the stage a replacement is executed.*/
@Serializable
enum class ReplacementPhase {
    /**Replaces values before the contents are parsed.*/
    FIRST,

    /**Replaces values after versioned comments have been evaluated and reassembled.*/
    LAST;
}

/**A replacement operation descriptor, which is serialized in a [ReplacementList].*/
@Serializable
sealed interface Replacement {
    /**The stored execution phase, being before or after parsing.*/
    val phase: ReplacementPhase
    /**Toggle key for the replacement. Non-null-identifier replacements are disabled by default,
     * and merged only if the key is present in the provided set.*/
    val identifier: String?
}

/**
 * Describes a literal string replacement, which optimises the operations and makes them order-independent.
 * During execution all values from [sources] are replaced with the [target].
 */
@Serializable
data class StringReplacement(
    val sources: MutableSet<String>,
    var target: String,
    override val phase: ReplacementPhase = ReplacementPhase.LAST,
    override val identifier: String? = null
) : Replacement {
    constructor(source: String, target: String, phase: ReplacementPhase = ReplacementPhase.LAST, identifier: String? = null)
            : this(mutableSetOf(source), target, phase, identifier)

    /**Represents the replacement as StringReplacement([identifier], [phase]) [[sources],... -> '[target]'].*/
    override fun toString(): String = buildString {
        append("StringReplacement(")
        if (identifier != null) append("$identifier, ")
        append("$phase) [${sources.joinToString { "'$it'" }} -> '$target']")
    }
}

/**
 * Describes a regex-pattern-based replacement, which allows for more fine-grained operations at the cost of performance and safety.
 * During the execution all occurrences of the [pattern] are replaced with the [target].
 */
@Serializable
class RegexReplacement(
    val pattern: @Serializable(with = RegexPatternsSerializer::class) Regex,
    val target: String,
    override val phase: ReplacementPhase,
    override val identifier: String?
) : Replacement {
    /**Represents the replacement as RegexReplacement([identifier], [phase]) [[pattern] -> '[target]'].*/
    override fun toString(): String = buildString {
        append("RegexReplacement(")
        if (identifier != null) append("$identifier, ")
        append("$phase) [${pattern.pattern} -> '$target']")
    }

    private object RegexPatternsSerializer : KSerializer<Regex> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Regex", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Regex) =
            encoder.encodeString(value.pattern)

        override fun deserialize(decoder: Decoder): Regex =
            decoder.decodeString().toRegex()
    }
}

/**A delegated collection of replacements, which performs checking and merging on added elements.*/
@JvmInline
@Serializable
value class ReplacementList(internal val delegate: MutableList<Replacement> = mutableListOf()) : MutableCollection<Replacement> by delegate {
    override fun addAll(elements: Collection<Replacement>): Boolean = if (elements.isEmpty()) false else {
        elements.forEach(::addReplacement)
        true
    }

    override fun add(element: Replacement): Boolean {
        addReplacement(element)
        return true
    }

    /**Adds the given replacement with [addString] or [addRegex] depending on its type.*/
    fun addReplacement(element: Replacement) = when (element) {
        is RegexReplacement -> addRegex(element.pattern, element.target, element.phase, element.identifier)
        is StringReplacement -> when (element.sources.size) {
            0 -> throw IllegalArgumentException("Replacement has no sources")
            1 -> addString(element.sources.first(), element.target, element.phase, element.identifier)
            else -> element.sources.forEach {
                addString(it, element.target, element.phase, element.identifier)
            }
        }
    }

    /**
     * Adds a [RegexReplacement] to the list with the matching parameters.
     * @throws IllegalArgumentException If [pattern] or [target] are empty
     * @throws IllegalArgumentException If [identifier] is not `null` and such identifier is already present in the list
     */
    fun addRegex(pattern: Regex, target: String, phase: ReplacementPhase = ReplacementPhase.LAST, identifier: String? = null) {
        require(pattern.pattern.isNotEmpty()) { "Can't replace empty pattern" }
        require(target.isNotEmpty()) { "Replacing with an empty string is not reversible" }
        if (isEmpty() || identifier != null) delegate += RegexReplacement(pattern, target, phase, identifier)
        else find { it.identifier == identifier }.let {
            if (it != null) throw IllegalArgumentException("Replacement '${identifier}' is already registered for $it")
            delegate += RegexReplacement(pattern, target, phase, null)
        }
    }

    /**
     * Adds a [StringReplacement] with the matching parameters or merges it with an existing entry if possible.
     * @throws IllegalArgumentException If [source] or [target] are empty
     * @throws IllegalArgumentException If the [identifier] is not null and the existing entry with the same one can't be merged.
     * @throws IllegalArgumentException If the provided parameters form a cyclical dependency or an ambiguous target.
     */
    fun addString(source: String, target: String, phase: ReplacementPhase = ReplacementPhase.LAST, identifier: String? = null) {
        fun addImpl() {
            delegate += StringReplacement(source, target, phase, identifier)
        }
        require(source.isNotEmpty()) { "Can't replace empty string" }
        require(target.isNotEmpty()) { "Replacing with an empty string is not reversible" }
        when {
            isEmpty() -> addImpl()
            identifier != null -> find { it.identifier == identifier }?.let {
                require(it is StringReplacement) { "Regex replacement '${identifier}' is already registered for $it" }
                require(it.phase == phase) { "Replacement '${identifier}' is already registered for $it with a different phase" }
                require(it.tryMerge(source, target)) { "Replacement '$source' -> '$target' couldn't be merged with $it" }
            } ?: addImpl()

            else -> {
                var merged = false
                for (it in this) when {
                    it.identifier != null || it.phase != phase -> continue
                    it is StringReplacement && it.tryMerge(source, target) -> {
                        merged = true; break
                    }
                }
                if (!merged) addImpl()
            }
        }
    }

    // TODO: Check for reversible replacements
    private fun StringReplacement.tryMerge(from: String, to: String): Boolean = when {
        from == target -> {
            require(to !in sources) { "Replacement '$from' -> '$to' forms a cycle with $this" }
            target = to; sources += from; true
        }

        to == target || to in sources -> {
            sources += from; true
        }

        from in sources -> {
            val message = "Replacement '$to' can't be replaced by both '$from' and $this"
            throw IllegalArgumentException(message)
        }

        else -> false
    }
}

/**Filters and merges [replacements] based on the [phase] and the enabled [tokens].*/
class ReplacementExecutor(replacements: ReplacementList, phase: ReplacementPhase, tokens: Set<String>) {
    private val matching = replacements.filter { it.phase == phase && (it.identifier == null || it.identifier in tokens) }
    private val regex = matching.filterIsInstance<RegexReplacement>()
    private val string = matching.mergeStringReplacements()
    private val lookup = string.flatMap { it.sources.map { s -> s to it.target } }.toMap()

    /**Performs configured replacements on the provided [text]. Returns a mutable [StringBuilder] to reduce string allocation.*/
    fun replace(text: CharSequence): CharSequence = if (matching.isEmpty()) text else StringBuilder(text)
        .replaceString()
        .replaceRegex()

    private fun Collection<Replacement>.mergeStringReplacements(): List<StringReplacement> {
        if (isEmpty()) return emptyList()

        val unassigned: ReplacementList
        val named: List<StringReplacement>
        filterIsInstance<StringReplacement>().groupBy { it.identifier != null }.let {
            unassigned = ReplacementList(it[false]?.toMutableList() ?: mutableListOf())
            named = it[true] ?: emptyList()
        }

        for (repl in named) for (src in repl.sources) unassigned.addString(src, repl.target, repl.phase, null)
        return unassigned.delegate as List<StringReplacement>
    }

    private fun StringBuilder.replaceString(): StringBuilder = also {
        for ((key, value) in lookup) {
            var index = indexOf(key)
            while (index >= 0) {
                replace(index, index + key.length, value)
                index += value.length
                index = indexOf(key, index)
            }
        }
    }

    private fun StringBuilder.replaceRegex(): StringBuilder {
        for (repl in regex) repl.pattern.replace(this, repl.target).let {
            replace(0, length, it)
        }
        return this
    }

    companion object {
        const val PREFIX = '~'

        /**
         * Looks up available tokens in the given file.
         * Token toggle comments must follow certain conditions to be included:
         * - The comment must start with the [PREFIX] and have an identifier string following it.
         * - The comment must be at the start of the file, with any non-empty or non-comment sequence terminating the search.
         *
         * Example:
         * ```java
         * /*~ token1*/  // valid
         * /*~ token2*/  // valid
         *
         * import java.util.*; // stops the lookup
         * /*~ token3*/  // ignored
         * ```
         */
        fun CharSequence.getReplacementTokens(recognizers: Iterable<CommentRecognizer>): Set<String> = buildSet {
            for (token in Scanner(this@getReplacementTokens, recognizers)) when (token.type as? ContentType) {
                ContentType.COMMENT_START, ContentType.COMMENT_END -> continue
                ContentType.CONTENT -> if (token.value.isNotBlank()) break
                ContentType.COMMENT -> if (token.value.getOrSpace(0) == PREFIX)
                    this += token.value.substring(1).trim()
                else -> break
            }
        }

        /**
         * Combines [getReplacementTokens] and [ReplacementExecutor.replace] operations.
         */
        fun CharSequence.replaceWithScannedTokens(replacements: ReplacementList, phase: ReplacementPhase, recognizers: Iterable<CommentRecognizer>): CharSequence {
            if (replacements.isEmpty() || isEmpty()) return this
            val tokens = getReplacementTokens(recognizers)
            val executor = ReplacementExecutor(replacements, phase, tokens)
            return executor.replace(this)
        }
    }
}