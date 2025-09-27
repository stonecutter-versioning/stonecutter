package dev.kikugie.stonecutter.build.param

import dev.kikugie.semver.data.Version
import dev.kikugie.stitcher.parse.adapter.ScannerAdapter
import dev.kikugie.stitcher.transform.TransformParameters
import dev.kikugie.stitcher.transform.replacement.Replacement
import dev.kikugie.stitcher.transform.strategy.CommentingStrategy
import dev.kikugie.stitcher.transform.strategy.SwappingStrategy
import dev.kikugie.stitcher.transform.strategy.UncommentingStrategy
import java.nio.file.Path
import kotlin.io.path.extension

private inline fun <T> Map<String, T>.get(ext: String, type: String) = checkNotNull(get(ext)) {
    "No $type registered for extension '.$ext'"
}

internal data class TransformParametersBuilder(
    val swaps: Map<String, String> = emptyMap(),
    val constants: Map<String, Boolean> = emptyMap(),
    val dependencies: Map<String, Version> = emptyMap(),
    val replacements: List<Replacement> = emptyList(),

    val scanners: Map<String, ScannerAdapter.Factory>,
    val commenters: Map<String, CommentingStrategy>,
    val uncommenters: Map<String, UncommentingStrategy>,
    val swappers: Map<String, SwappingStrategy>,
) : java.io.Serializable {
    operator fun contains(file: Path) = file.extension in scanners

    fun forFile(file: Path): TransformParameters {
        val extension = file.extension
        val scanner = scanners.get(extension, "comment scanner")
        val commenter = commenters.get(extension, "commenter")
        val uncommenter = uncommenters.get(extension, "uncommenter")
        val swapper = swappers.get(extension, "swapping strategy")

        return TransformParameters(scanner, commenter, uncommenter, swapper, swaps, constants, dependencies, replacements)
    }
}