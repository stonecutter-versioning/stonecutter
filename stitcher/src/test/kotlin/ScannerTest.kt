import dev.kikugie.stitcher.antlr.scanner.HashStyleScanner
import dev.kikugie.stitcher.antlr.scanner.SlashStyleScanner
import dev.kikugie.stitcher.issue.ProblemSink
import dev.kikugie.stitcher.parse.adapter.ScannerAdapter
import dev.kikugie.stitcher.parse.builder.LayoutBuilder
import dev.kikugie.stitcher.util.FileLineIndex
import dev.kikugie.stitcher.util.asSequence
import dev.kikugie.stitcher.util.toStream
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldBeOneOf
import io.kotest.matchers.shouldBe
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.Lexer
import org.antlr.v4.runtime.Token
import kotlin.io.path.Path
import kotlin.reflect.KClass

private inline fun <reified T : Lexer> spec(opener: Int, closer: Int) =
    ScannerTestSpec(T::class, intArrayOf(opener), intArrayOf(closer))

private inline fun <reified T : Lexer> spec(openers: IntArray, closers: IntArray) =
    ScannerTestSpec(T::class, openers, closers)

private fun createDummySink(input: CharStream) = ProblemSink(Path(""), FileLineIndex(input)) { _, at, it ->
    System.err.println("$at $it")
}

private fun Token.layoutName(): String = LayoutBuilder.VOCABULARY.getDisplayName(type)

private class ScannerTestSpec(val lexer: KClass<out Lexer>, val openers: IntArray, val closers: IntArray) : ScannerAdapter.Factory {
    val constructor by lazy { lexer.java.getConstructor(CharStream::class.java) }

    override fun create(input: CharStream, sink: ProblemSink): ScannerAdapter =
        ScannerAdapter(constructor.newInstance(input), openers, closers, sink)

    infix fun test(input: String) {
        val stream: CharStream = input.toStream()
        val tokens: Sequence<Token> = create(stream, createDummySink(stream)).asSequence()
        for ((a, b) in tokens.zipWithNext()) checkTokenPair(a, b)
    }

    private fun checkTokenPair(a: Token, b: Token) = when (a.type) {
        LayoutBuilder.CONTENT -> withClue("After CONTENT '${a.text}' with '${b.text}'") {
            b.layoutName() shouldBe "COMMENT_OPEN"
        }

        LayoutBuilder.COMMENT_OPEN -> withClue("After COMMENT_OPEN '${a.text}' with '${b.text}'") {
            b.layoutName() shouldBe "COMMENT_BODY"
        }

        LayoutBuilder.COMMENT_BODY -> withClue("After COMMENT_BODY '${a.text}' with '${b.text}'") {
            b.layoutName() shouldBe "COMMENT_CLOSE"
        }

        LayoutBuilder.COMMENT_CLOSE -> withClue("After COMMENT_CLOSE '${a.text}' with '${b.text}'") {
            b.layoutName() shouldBeOneOf listOf("CONTENT", "COMMENT_OPEN")
        }

        else -> {
            a.layoutName() shouldBeIn LayoutBuilder.TOKEN_NAMES
        }
    }
}

class ScannerTest : FunSpec({
    test("double slash") {
        spec<SlashStyleScanner>(SlashStyleScanner.SLASH_COMMENT_START, SlashStyleScanner.SLASH_COMMENT_END) test """
            normal content
            // line comment
            // line // comment
            "quoted // symbol
            still string" // comment
            //
        """.trimIndent()
    }

    test("slash star") {
        spec<SlashStyleScanner>(SlashStyleScanner.STAR_COMMENT_START, SlashStyleScanner.STAR_COMMENT_END) test """
            normal content
            /*
            multiline comment
            */
            "slash star /* in string"
            star slash */ in text
        """.trimIndent()
    }

    test("hash") {
        spec<HashStyleScanner>(HashStyleScanner.HASH_COMMENT_START, HashStyleScanner.HASH_COMMENT_END) test """
            normal content
            # comment
            "hash # in string"
        """.trimIndent()
    }

    // TODO: test in-string cases better
})