package dev.kikugie.stitcher.issue

import dev.kikugie.stitcher.data.StitcherToken
import dev.kikugie.stitcher.util.AntlrToken

internal fun ProblemSource.at(token: StitcherToken): ProblemLocation =
    at(token.range.first)

internal fun ProblemSource.at(token: AntlrToken): ProblemLocation =
    at(token.line, token.charPositionInLine + 1)

internal inline fun <T : Any> throwRun(provider: () -> T, handler: (e: Exception) -> Nothing): T = try {
    provider()
} catch (bail: BailException) {
    throw bail
} catch (e: Exception) {
    handler(e)
}

internal inline fun <T : Any> tryRun(provider: () -> T, handler: (e: Exception) -> Unit): T? = try {
    provider()
} catch (_: BailException) {
    null
} catch (e: Exception) {
    handler(e); null
}