import dev.kikugie.commons.collections.present
import dev.kikugie.stitcher.antlr.LayoutParser
import dev.kikugie.stitcher.antlr.converter.BlockBuilder
import dev.kikugie.stitcher.antlr.scanner.SlashStyleScanner
import dev.kikugie.stitcher.api.ScannerBuilder
import dev.kikugie.stitcher.api.parameters
import dev.kikugie.stitcher.api.scanner
import dev.kikugie.stitcher.debug.PresentationBuilder
import dev.kikugie.stitcher.debug.PresentationCollector
import dev.kikugie.stitcher.transformer.ReplaceTransformation
import dev.kikugie.stitcher.transformer.SourceTransformation
import dev.kikugie.stitcher.transformer.TransformationBuilder
import io.kotest.core.spec.style.AnnotationSpec
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.Token

class Experiments : AnnotationSpec() {
    private fun ScannerBuilder.defaultTokens() {
        openingTokens += SlashStyleScanner.SLASH_COMMENT_START
        openingTokens += SlashStyleScanner.STAR_COMMENT_START

        closingTokens += SlashStyleScanner.SLASH_COMMENT_END
        closingTokens += SlashStyleScanner.STAR_COMMENT_END
    }

    @Test
    fun presentation() {
        val factory = scanner {
            defaultTokens()
            lexer(::SlashStyleScanner)
        }

        val input = CharStreams.fromFileName("/home/kikugie/IdeaProjects/stonecutter-template-fabric/src/main/java/com/example/TemplateMod.java")
        val adapter = factory.create(input)

        val parser = LayoutParser(CommonTokenStream(adapter))
        val context = parser.file()
        val tree = context.accept(BlockBuilder)
        val presentation = tree.accept(PresentationCollector)
        println(presentation.accept(PresentationBuilder()))
    }

    @Test
    fun transformation() {
        val factory = scanner {
            defaultTokens()
            lexer(::SlashStyleScanner)
        }

        val input = """
            /*$ my_swap*/
                - hello
                - I can't hear you!
        """.trimIndent()

        val parameters = parameters {
            swaps["my_swap"] = "- goodbye"
        }

        val tree = factory.create(CharStreams.fromString(input))
            .let(::CommonTokenStream)
            .let(::LayoutParser)
            .let(LayoutParser::file)
            .accept(BlockBuilder)

        val transformer = TransformationBuilder(parameters)
        tree.accept(transformer)

        println(transformer.changes.present())
        println(transformer.changes.single().let {
            it as ReplaceTransformation
            input.replaceRange(it.sourceRange, it.value)
        })
    }

    @Test
    fun kotlin() {
        val factory = scanner {
            defaultTokens()
            lexer { SlashStyleScanner(it).apply { nestMultiLineComments = true } }
        }

        val input = """
            /* outer
                /* inner */
            */
        """.trimIndent()

        val adapter = factory.create(CharStreams.fromString(input))
        val tokens = generateSequence { adapter.nextToken().takeIf { it.type != Token.EOF } }
        for (it in tokens) {
            val name = LayoutParser.VOCABULARY.getDisplayName(it.type)
            println("$name: '${it.text.replace("\n", "\\n")}'")
        }
    }
}