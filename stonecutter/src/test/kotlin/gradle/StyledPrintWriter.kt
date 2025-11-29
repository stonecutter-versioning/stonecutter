package gradle

import com.github.ajalt.mordant.rendering.TextStyle
import java.io.PrintStream
import java.io.Writer

class StyledPrintWriter(val style: TextStyle) : Writer() {
    private val delegate: PrintStream = System.out

    override fun write(cbuf: CharArray, off: Int, len: Int) {
        val text = String(cbuf, off, len).lineSequence()
            .map(style::invoke)
            .joinToString("\n")
        delegate.print(text)
    }

    override fun flush() = delegate.flush()
    override fun close() = delegate.close()
}