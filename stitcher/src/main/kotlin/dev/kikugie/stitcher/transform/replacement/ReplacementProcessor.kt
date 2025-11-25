package dev.kikugie.stitcher.transform.replacement

import dev.kikugie.commons.takeAs
import dev.kikugie.stitcher.data.composite.CodeBlock
import dev.kikugie.stitcher.data.composite.ReplacementDefinition
import dev.kikugie.stitcher.issue.ProblemSource
import dev.kikugie.stitcher.issue.at
import dev.kikugie.stitcher.util.buildString

internal class ReplacementProcessor(val replacements: List<Replacement>, problems: ProblemSource) : ProblemSource by problems {
    private val builder: ReplacementBuilder<Replacement> by lazy { ReplacementBuilder(replacements.filter { it.identifier == null }) }
    private val entries: List<Replacement> by lazy { builder.build() }
    private lateinit var executor: ReplacementExecutor

    operator fun plusAssign(host: CodeBlock) {
        if (::executor.isInitialized)
            at(host.marker) report "Late replacement token"

        val identifier = host.definition.takeAs<ReplacementDefinition>().identifier
        val matches = replacements.filter { it.identifier == identifier.text }
        if (matches.isEmpty())
            at(identifier) report "Unresolved replacement identifier"

        for (match in matches) try {
            builder.add(match.asAnonymous())
        } catch (e: ReplacementException) {
            at(identifier) report e.issue // TODO: Should it add the details?
        }
    }

    fun finalize() {
        if (!::executor.isInitialized)
            executor = ReplacementExecutor(entries)
    }

    fun replace(content: String): String? {
        finalize()

        return if (entries.isEmpty()) null
        else buildString(content, executor::replace)
    }
}