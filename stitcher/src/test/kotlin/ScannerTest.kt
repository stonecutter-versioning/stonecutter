import dev.kikugie.commons.text.getOrDefault
import dev.kikugie.stitcher.antlr.scanner.SlashStyleScanner
import dev.kikugie.stitcher.util.asSequence
import dev.kikugie.stitcher.util.toStream
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.engine.stable.IsStableType
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.string.shouldBeEmpty
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.Lexer

@IsStableType
private data class ScanSpec(
    val lexer: Lexer,
    val openers: List<Int>,
    val closers: List<Int>,
    val pattern: String
) {
    override fun toString(): String = "${lexer::class.simpleName}: [$pattern]"
    fun run() {
        var hasAtLeastOne = false
        for ((i, token) in lexer.asSequence().withIndex()) {
            hasAtLeastOne = true
            withClue("At $i: $token") {
                when (pattern.getOrDefault(i)) {
                    '+' -> token.type.shouldBeIn(openers)
                    '-' -> token.type.shouldBeIn(closers)
                    else -> i shouldBeLessThan pattern.length
                }
            }
        }
        if (!hasAtLeastOne) pattern.shouldBeEmpty()
    }

    class Template(val lexer: (CharStream) -> Lexer, val openers: IntArray, val closers: IntArray) {
        context(collector: SequenceScope<ScanSpec>) suspend fun yield(pattern: String, text: String) =
            collector.yield(ScanSpec(lexer(text.toStream()), openers.toList(), closers.toList(), pattern))
    }
}

class ScannerTest : FunSpec({
    context("java-like") {
        withData(sequence {
            val template = ScanSpec.Template(::SlashStyleScanner,
                intArrayOf(SlashStyleScanner.SLASH_COMMENT_START, SlashStyleScanner.STAR_COMMENT_START),
                intArrayOf(SlashStyleScanner.SLASH_COMMENT_END, SlashStyleScanner.STAR_COMMENT_END))

            template.yield("+-", """
                // comment
                content
            """.trimIndent())

            template.yield("+-", "/* comment */")
            template.yield("", "\"/* comment */\"")
            template.yield("", "\"\"\"/* comment */\"\"\"")
            template.yield("+-", "\"\"/* comment */\"\"")
            template.yield("+-+-", "/* /* */ /* */ */")
        }) {
            it.run()
        }
    }
})