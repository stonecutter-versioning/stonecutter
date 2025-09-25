package dev.kikugie.stitcher

import dev.kikugie.stitcher.issue.ProblemSink
import dev.kikugie.stitcher.parse.builder.LayoutBuilder
import dev.kikugie.stitcher.parse.inline.InlineErrorListener
import dev.kikugie.stitcher.parse.inline.InlineTokenConverter
import dev.kikugie.stitcher.transform.BlockTransformer
import dev.kikugie.stitcher.transform.RuntimeState
import dev.kikugie.stitcher.transform.TransformParameters
import dev.kikugie.stitcher.transform.visitor.BlockAssembler.Companion.join
import dev.kikugie.stitcher.util.errorListener
import dev.kikugie.stitcher.util.toStream
import org.antlr.v4.runtime.CommonTokenStream
import java.nio.file.Path

// TODO: Do what the errors say
public fun process(file: Path, contents: String, parameters: TransformParameters): String {
    val input = contents.toStream()
    val runtime = RuntimeState(input, ProblemSink(file))
    val source = parameters.adapter.create(input, runtime.sink).apply {
        scanner.errorListener(InlineErrorListener(runtime.sink, 0))
    }
    val layout = LayoutBuilder.build(CommonTokenStream(source), runtime.sink, InlineTokenConverter.DEFAULT)
    if (!runtime.sink.isSuccess) error("We had errors, please implement graceful exit")

    val transformer = BlockTransformer(runtime, parameters, InlineTokenConverter.DEFAULT)
    val modified = layout.accept(transformer)
    if (!runtime.sink.isSuccess) error("We had more errors, please implement graceful exit")
    return modified.join()
}