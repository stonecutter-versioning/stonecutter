package dev.kikugie.stitcher.debug

import com.github.ajalt.mordant.rendering.TextColors.*

private val TokenPresentation.kind get() = brightMagenta(type)
private val TokenPresentation.bounds get() = brightMagenta("[${range.first},${range.last})")

class PresentationBuilder() : TokenPresentation.Visitor<String> {
    private var indent = 0

    override fun visitLeaf(it: TokenPresentation.Leaf) = visitGeneric(it)
    override fun visitGeneric(it: TokenPresentation) =
        "${it.kind}${magenta("@")}${it.bounds} '${brightGreen(it.value)}'"

    override fun visitStruct(it: TokenPresentation.Struct) = buildString {
        appendLine("${it.kind}${magenta("@")}${it.bounds}")
        indent++
        val seen = mutableSetOf<String>()
        val children = it.children.toList()
        var count = 0
        children.joinTo(this, "\n") { (key, value) ->
            count++
            val str = value.accept(this@PresentationBuilder)
            val name = if (seen.add(key)) cyan(key) else white(key)
            "${indent(indent, count == children.size)} $name: $str"
        }
        indent--
    }

    private fun indent(n: Int, last: Boolean) = gray(buildString {
        if (n > 1) append("| ".repeat(n - 1))
        if (last) append("\\-") else append("|-")
    })
}