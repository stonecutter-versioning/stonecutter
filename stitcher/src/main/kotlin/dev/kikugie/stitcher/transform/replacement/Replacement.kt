package dev.kikugie.stitcher.transform.replacement

import kotlinx.serialization.Serializable
import org.intellij.lang.annotations.Language

@Serializable
public sealed interface Replacement : java.io.Serializable {
    public val identifier: String?
}

@Serializable @JvmRecord
public data class StringReplacement(
    val target: String,
    val sources: Set<String>,
    override val identifier: String? = null,
) : Replacement {
    public constructor(target: String, vararg sources: String, identifier: String? = null)
        : this(target, sources.toSet(), identifier)
}

@Serializable @JvmRecord
public data class RegexReplacement(
    val target: String,
    val pattern: String,
    val flags: Set<RegexOption> = emptySet(),
    override val identifier: String? = null,
) : Replacement {
    public constructor(target: String, @Language("RegExp") pattern: String, vararg options: RegexOption, identifier: String? = null)
        : this(target, pattern, options.toSet(), identifier)
    val regex: Regex get() = Regex(pattern, flags)
}

internal fun Replacement.asAnonymous(): Replacement = when(this) {
    is StringReplacement -> copy(identifier = null)
    is RegexReplacement -> copy(identifier = null)
}