package dev.kikugie.stitcher.transformer

import dev.kikugie.commons.text.countMatching

private fun buildScopeModel(text: String): ScopeModel {
    var seenContent = false
    var commonIndent: String? = null
    val prefixBuilder = StringBuilder()
    val linesBuilder = mutableListOf<String>()
    for (line in text.lineSequence()) {
        val indentLength = line.countMatching(' ', '\t')
        val contentText = line.substring(indentLength)

        if (contentText.isNotEmpty())
            seenContent = true

        if (!seenContent) {
            prefixBuilder.appendLine(line.take(indentLength))
            continue
        }

        if (commonIndent == null || indentLength < commonIndent.length)
            commonIndent = line.take(indentLength)

        linesBuilder += line
    }

    val postfix = linesBuilder.lastOrNull()
        ?.takeIf { it.isBlank() }
        ?.also { linesBuilder.removeLast() }
        .orEmpty()

    return ScopeModel(prefixBuilder.toString(), postfix, commonIndent.orEmpty(), linesBuilder.toList())
}

data class ScopeModel(
    val prefix: String,
    val postfix: String,
    val indent: String,
    val lines: List<String>
) {
    companion object {
        operator fun invoke(text: String): ScopeModel = buildScopeModel(text)
    }
}
