package dev.kikugie.stonecutter.build.param

import dev.kikugie.semver.data.Version
import dev.kikugie.stitcher.parser.adapter.ScannerAdapter
import dev.kikugie.stitcher.transform.TransformParameters
import dev.kikugie.stitcher.transform.replacement.Replacement
import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.controller.file.FileHandlerBuilder
import dev.kikugie.stonecutter.controller.file.ScannerBuilder
import dev.kikugie.stonecutter.util.get
import kotlinx.serialization.Serializable
import org.gradle.api.provider.MapProperty
import java.nio.file.Path
import kotlin.io.path.extension

private fun ScannerBuilder.toFactory(): ScannerAdapter.Factory {
    val constructor = lexer.get()
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
    internal fun forFile(file: Path, handlers: MapProperty<String, FileHandlerBuilder>): TransformParameters? {
        val handler = handlers[file.extension.lowercase()].orNull ?: return null
        val scanner = handler.scanner.get().toFactory()
        val commenter = handler.commenter.get()
        val uncommenter = handler.uncommenter.get()
        val swapper = handler.swapper.get()

        return TransformParameters(scanner, commenter, uncommenter, swapper, swaps, constants, dependencies, replacements)
    }
}
