package dev.kikugie.stitcher.debug

internal fun DebugView.join(): String = buildString {
    DebugViewPrinter(this).accept(this@join)
}

private class DebugViewPrinter(val builder: StringBuilder) {
    private var indent: Int = 0
    private val prefix: String get() = "  ".repeat(indent)

    fun accept(view: DebugView) {
        appendLine("${view.name}:")
        if (view.properties.isNotEmpty()) indented {
            appendLine("properties:")
            indented {
                for ((k, v) in view.properties) {
                    appendLine("$k = ${v.replaceIndent(prefix).trimStart()}")
                }
            }
        }
        if (view.attributes.isNotEmpty()) indented {
            appendLine("attributes:")
            indented {
                for ((k, v) in view.attributes) {
                    appendLine("$k:")
                    accept(v)
                }
            }
        }
        if (view.items.isNotEmpty()) indented {
            appendLine("items:")
            indented {
                for (it in view.items) {
                    accept(it)
                }
            }
        }
    }

    private fun appendLine(value: String) = builder.appendLine(prefix + value)
    private inline fun indented(action: () -> Unit) {
        indent += 1
        action()
        indent -= 1
    }
}