package dev.kikugie.stitcher

import dev.kikugie.stitcher.issue.ProblemReporter
import dev.kikugie.stitcher.issue.ProblemSink
import dev.kikugie.stitcher.parse.builder.LayoutBuilder
import dev.kikugie.stitcher.parse.inline.InlineErrorListener
import dev.kikugie.stitcher.parse.inline.InlineTokenConverter
import dev.kikugie.stitcher.transform.BlockTransformer
import dev.kikugie.stitcher.transform.RuntimeState
import dev.kikugie.stitcher.transform.TransformParameters
import dev.kikugie.stitcher.transform.visitor.BlockAssembler.Companion.join
import dev.kikugie.stitcher.util.FileLineIndex
import dev.kikugie.stitcher.util.errorListener
import dev.kikugie.stitcher.util.toStream
import org.antlr.v4.runtime.CommonTokenStream
import java.nio.file.Path

public fun process(file: Path, contents: String, parameters: TransformParameters, reporter: ProblemReporter): String {
    val input = contents.toStream()
    val index = FileLineIndex(input)
    val runtime = RuntimeState(input, ProblemSink(file, index, reporter))
    val source = parameters.adapter.create(input, runtime.sink).apply {
        scanner.errorListener(InlineErrorListener(runtime.sink, runtime.sink.index, 0))
    }
    val layout = LayoutBuilder.build(CommonTokenStream(source), runtime.sink, InlineTokenConverter.DEFAULT)
    check(runtime.sink.isSuccess) { "Parsing error. See log for more details" }

    val transformer = BlockTransformer(runtime, parameters, InlineTokenConverter.DEFAULT)
    val modified = layout.accept(transformer)
    check(runtime.sink.isSuccess) { "Transformation error. See log for more details" }
    return modified.join()
}