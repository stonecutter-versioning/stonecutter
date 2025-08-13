import dev.kikugie.stitcher.antlr.LayoutParser
import dev.kikugie.stitcher.antlr.converter.BlockBuilder
import dev.kikugie.stitcher.antlr.scanner.SlashStyleScanner
import dev.kikugie.stitcher.api.scanner
import dev.kikugie.stitcher.debug.PresentationBuilder
import dev.kikugie.stitcher.debug.PresentationCollector
import io.kotest.core.spec.style.AnnotationSpec
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

class Experiments : AnnotationSpec() {
    @Test
    fun exp() {
        val factory = scanner {
            openingTokens += SlashStyleScanner.SLASH_COMMENT_START
            openingTokens += SlashStyleScanner.STAR_COMMENT_START

            closingTokens += SlashStyleScanner.SLASH_COMMENT_END
            closingTokens += SlashStyleScanner.STAR_COMMENT_END

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
}