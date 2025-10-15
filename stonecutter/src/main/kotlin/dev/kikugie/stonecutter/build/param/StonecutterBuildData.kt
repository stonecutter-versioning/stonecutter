package dev.kikugie.stonecutter.build.param

import dev.kikugie.semver.data.Version
import dev.kikugie.stitcher.parser.adapter.ScannerAdapter
import dev.kikugie.stitcher.transform.TransformParameters
import dev.kikugie.stitcher.transform.replacement.Replacement
import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.controller.file.FileHandlerContainer
import dev.kikugie.stonecutter.controller.file.ScannerBuilder
import dev.kikugie.stonecutter.controller.file.StonecutterExperimentalFilesAPI
import kotlinx.serialization.Serializable
import java.nio.file.Path
import kotlin.io.path.extension

@OptIn(StonecutterExperimentalFilesAPI::class)
private fun ScannerBuilder.toFactory(): ScannerAdapter.Factory {
    val constructor = constructor.get()
    val openers = openers.get().toIntArray()
    val closers = closers.get().toIntArray()
    return ScannerAdapter.Factory { input, sink -> ScannerAdapter(constructor.create(input), openers, closers, sink) }
}

/**
 * Represents the build data serialized in `node.json`.
 */
@Serializable @JvmRecord
public data class StonecutterBuildData(
    public val constants: Map<Identifier, Boolean>,
    public val swaps: Map<Identifier, String>,
    public val dependencies: Map<Identifier, Version>,
    public val replacements: List<Replacement>,
) : java.io.Serializable {
    @OptIn(StonecutterExperimentalFilesAPI::class)
    internal fun forFile(file: Path, handlers: FileHandlerContainer): TransformParameters? = synchronized(handlers) {
        val handler = handlers[file.extension] ?: return null
        val scanner = handler.scanner.map(ScannerBuilder::toFactory).get()
        val commenter = handler.commenter.get()
        val uncommenter = handler.uncommenter.get()
        val swapper = handler.swapper.get()

        TransformParameters(scanner, commenter, uncommenter, swapper, swaps, constants, dependencies, replacements)
    }
}
