package dev.kikugie.stitcher.issue

import org.antlr.v4.runtime.Token

internal fun ProblemTemplate.at(token: Token): ProblemTemplate = at(token.line, token.charPositionInLine)

internal data class ProblemTemplate(val message: String, val severity: ProblemSeverity = ERROR, val location: ProblemLocation? = null) {
    fun at(line: Int, offset: Int) = copy(location = ProblemLocation.Direct(line, offset))
    fun at(index: Int) = copy(location = ProblemLocation.Lazy(index))
    fun format(vararg args: Any?) = copy(message = message.format(*args))

    fun build(): ProblemReport = ProblemReport(message, checkNotNull(location), severity)
}