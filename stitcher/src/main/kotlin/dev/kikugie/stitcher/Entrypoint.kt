package dev.kikugie.stitcher

import dev.kikugie.stitcher.antlr.InlineErrorListener
import dev.kikugie.stitcher.data.eval.BlockToStringVisitor.Companion.join
import dev.kikugie.stitcher.issue.BailException
import dev.kikugie.stitcher.issue.ProblemCause
import dev.kikugie.stitcher.issue.ProblemConsumer
import dev.kikugie.stitcher.issue.ProblemLocation
import dev.kikugie.stitcher.issue.ProblemSource
import dev.kikugie.stitcher.parser.StitcherTokenFactory
import dev.kikugie.stitcher.parser.layout.LayoutParser
import dev.kikugie.stitcher.transform.BlockTransformer
import dev.kikugie.stitcher.transform.RuntimeState
import dev.kikugie.stitcher.transform.TransformParameters
import dev.kikugie.stitcher.transform.impl.InvalidArgChecker
import dev.kikugie.stitcher.util.FileLineIndex
import dev.kikugie.stitcher.util.errorListener
import dev.kikugie.stitcher.util.toStream
import org.antlr.v4.runtime.CommonTokenStream
import java.nio.file.Path
import kotlin.io.path.absolutePathString

public fun process(file: Path, contents: String, parameters: TransformParameters, reporter: ProblemConsumer): String {
    val input = contents.toStream(file.absolutePathString())
    val index = FileLineIndex(input)
    val problems = ProblemStorage(file, index, reporter)
    val runtime = RuntimeState(input, problems, parameters)
    val source = parameters.adapter.create(input, runtime.problems).apply {
        scanner.errorListener(InlineErrorListener(runtime.problems, ProblemLocation(1, 1)))
    }
    val layout = LayoutParser.parse(CommonTokenStream(source), input, runtime.problems, StitcherTokenFactory)
    if (problems.hasFailed) {
        layout.accept(InvalidArgChecker(parameters, problems))
        throw BailException()
    }

    val transformer = BlockTransformer(runtime, parameters, StitcherTokenFactory)
    val modified = layout.accept(transformer)
    if (problems.hasFailed) throw BailException()

    return modified.join()
}

private class ProblemStorage(
    val file: Path,
    val index: FileLineIndex,
    val consumer: ProblemConsumer
) : ProblemSource {
    override var hasFailed: Boolean = false
        private set

    override fun at(index: Int): ProblemLocation = this.index.locate(index)
    override fun at(line: Int, column: Int): ProblemLocation = ProblemLocation(line, column)
    override fun problem(message: String, exception: Throwable?): ProblemCause = ProblemCause(message, exception)

    override fun accept(location: ProblemLocation, cause: ProblemCause) {
        hasFailed = true
        consumer.accept(file, location, cause)
    }
}