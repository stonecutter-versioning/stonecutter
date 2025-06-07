package dev.kikugie.stonecutter.process

import dev.kikugie.stitcher.data.replacement.ReplacementExecutor.Companion.replaceWithScannedTokens
import dev.kikugie.stitcher.data.replacement.ReplacementPhase
import dev.kikugie.stitcher.data.scope.Scope
import dev.kikugie.stitcher.eval.join
import dev.kikugie.stitcher.exception.ErrorHandler
import dev.kikugie.stitcher.exception.StoringErrorHandler
import dev.kikugie.stitcher.exception.join
import dev.kikugie.stitcher.parser.FileParser
import dev.kikugie.stitcher.transformer.Transformer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.*

@OptIn(ExperimentalPathApi::class)
internal class FileProcessor(private val params: ProcessParameters) {
    private val inPlace = params.dirs.input == params.dirs.output

    internal companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(FileProcessor::class.java)
        fun Path.withExtension(ext: String): Path =
            parent.resolve("${fileName.nameWithoutExtension}.$ext")

        fun Path.resolveChecked(subpath: Path): Path = resolve(subpath).apply {
            parent.createDirectories()
        }
    }

    inner class EntryProcessor(private val source: Path) {
        internal val collector = LogCollector(LOGGER, params.dirs.input.resolve(source), params.debug)

        fun process(): CharSequence? {
            params.statistics.total++
            if (!params.filter(source)) return null logging "Skipping, filtered"
            var text: CharSequence = params.dirs.input.resolve(source).readText(params.charset)

            val handler = StoringErrorHandler()
            params.statistics.processed++

            val replacements = params.parameters.replacements
            var result: CharSequence = text
            result = result.replaceWithScannedTokens(replacements, ReplacementPhase.FIRST, params.recognizers)
            val parser = FileParser.create(result, handler, params.recognizers, params.parameters)
            val ast = parser.parse()
            collector.push("Parsed AST, ${handler.errors.size} errors")
            handler.throwIfHasErrors()

            Transformer(ast, params.recognizers, params.parameters, handler).process()
            collector.push("Transformed AST, ${handler.errors.size} errors")
            handler.throwIfHasErrors()

            result = ast.join().replaceWithScannedTokens(replacements, ReplacementPhase.LAST, params.recognizers)
            return if (result == text) null logging "Skipping, matches input"
            else result logging "Successfully processed"
        }

        private inline fun <T, R> T.runReporting(message: String, action: T.() -> R): R? = kotlin.runCatching {
            action()
        }.onFailure {
            collector.push(message, it)
        }.getOrNull()

        private infix fun <T> T.logging(message: String) = also {
            collector.push(message)
        }

        private fun ErrorHandler.throwIfHasErrors(): Nothing? {
            if (errors.isEmpty()) return null
            val path = params.dirs.input.resolve(source)
            throw ProcessException("Failed to parse ${path.absolutePathString()}").apply {
                errors.forEach { addSuppressed(ProcessException(it.join())) }
            }
        }
    }

    fun process(): () -> Unit {
        if (params.dirs.temp.exists()) params.dirs.temp.deleteRecursively()

        val errors = mutableMapOf<Path, Throwable>()
        params.dirs.input.walk().forEach {
            processEntry(it)?.also { e -> errors[it] = e }
        }

        if (errors.isNotEmpty()) composeErrors(params.dirs.input, errors)
        return ::applyTemp
    }

    private fun processEntry(file: Path): Throwable? {
        val relative = params.dirs.input.relativize(file)
        val processor = EntryProcessor(relative)
        val result = try {
            processor.process()
        } catch (e: Exception) {
            processor.collector.release()
            return e
        }

        return runCatching {
            saveToTemp(relative, result, file)
        }.also {
            processor.collector.release()
        }.exceptionOrNull()
    }

    private fun saveToTemp(relative: Path, it: CharSequence?, file: Path) {
        val dest = params.dirs.temp.resolveChecked(relative)
        if (it != null) dest.writeText(it, params.charset, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        else if (!inPlace) file.copyTo(dest, true)
    }

    private fun composeErrors(root: Path, errors: Map<Path, Throwable>): Nothing =
        throw ProcessException("Failed to process files in ${root.absolutePathString()}").apply {
            for ((_, err) in errors) addSuppressed(err)
        }

    private fun applyTemp() {
        if (params.dirs.temp.notExists()) return
        params.dirs.output.createDirectories()
        params.dirs.temp.copyToRecursively(params.dirs.output, followLinks = false, overwrite = true, onError = { from, to, e ->
            throw IOException("Failed to copy $from to $to", e)
        })
    }
}