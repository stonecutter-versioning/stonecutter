import dev.kikugie.semver.data.SemanticVersion
import dev.kikugie.stitcher.antlr.scanner.SlashStyleScanner
import dev.kikugie.stitcher.issue.ProblemSink
import dev.kikugie.stitcher.parse.inline.InlineTokenConverter
import dev.kikugie.stitcher.parse.adapter.ScannerAdapter
import dev.kikugie.stitcher.parse.builder.LayoutBuilder
import dev.kikugie.stitcher.transform.visitor.BlockAssembler.Companion.join
import dev.kikugie.stitcher.transform.BlockTransformer
import dev.kikugie.stitcher.transform.RuntimeState
import dev.kikugie.stitcher.transform.TransformParameters
import dev.kikugie.stitcher.transform.impl.StandardSwapStrategy
import dev.kikugie.stitcher.transform.impl.StarCommentStrategy
import dev.kikugie.stitcher.transform.replacement.StringReplacement
import dev.kikugie.stitcher.util.toStream
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CommonTokenStream
import kotlin.io.path.Path
import kotlin.io.path.readText

class MoreExperiments : AnnotationSpec() {
    @Test fun `try and see`() {
        val input = Path("/home/kikugie/IdeaProjects/stonecutter-template-fabric/src/main/java/com/example/TemplateMod.java")
        val original = input.readText()
        val runtime = RuntimeState(input.toStream(), ProblemSink(input))
        val params = TransformParameters(
            ::createAdapter,
            StarCommentStrategy,
            StarCommentStrategy,
            StandardSwapStrategy,
            mapOf("mod_version" to "\"1.0!\";", "minecraft" to "\"2.0\";"),
            mapOf("release" to true),
            mapOf("fapi" to SemanticVersion(intArrayOf(0, 128)), "" to SemanticVersion(intArrayOf(1))),
            listOf(StringReplacement("net.new.", "net."))
        )

        val source = params.adapter.create(runtime.input, runtime.sink)
        val layout = LayoutBuilder.build(CommonTokenStream(source), runtime.sink, InlineTokenConverter.DEFAULT)

        val read = layout.join()
        read shouldBe original

        val transformer = BlockTransformer(runtime, params, InlineTokenConverter.DEFAULT)
        val modified = layout.accept(transformer)
        println(modified.join())
    }

    private fun createAdapter(input: CharStream, sink: ProblemSink): ScannerAdapter = ScannerAdapter(
        SlashStyleScanner(input),
        intArrayOf(SlashStyleScanner.SLASH_COMMENT_START, SlashStyleScanner.STAR_COMMENT_START),
        intArrayOf(SlashStyleScanner.SLASH_COMMENT_END, SlashStyleScanner.STAR_COMMENT_END),
        sink
    )
}