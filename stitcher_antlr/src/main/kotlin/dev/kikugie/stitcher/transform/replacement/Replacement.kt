package dev.kikugie.stitcher.transform.replacement

import org.intellij.lang.annotations.Language

public sealed interface Replacement {
    public val identifier: String?
}

public data class StringReplacement(
    val target: String,
    val sources: Set<String>,
    override val identifier: String? = null,
) : Replacement {
    public constructor(target: String, vararg sources: String, identifier: String? = null)
        : this(target, sources.toSet(), identifier)
}

public data class RegexReplacement(
    val target: String,
    val pattern: Regex,
    override val identifier: String? = null,
) : Replacement {
    public constructor(target: String, @Language("RegExp") pattern: String, vararg options: RegexOption, identifier: String? = null)
        : this(target, Regex(pattern, options.toSet()), identifier)
}