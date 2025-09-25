package dev.kikugie.stitcher.transform

import dev.kikugie.semver.data.Version
import dev.kikugie.stitcher.parse.adapter.ScannerAdapter
import dev.kikugie.stitcher.transform.replacement.Replacement
import dev.kikugie.stitcher.transform.strategy.CommentingStrategy
import dev.kikugie.stitcher.transform.strategy.SwappingStrategy
import dev.kikugie.stitcher.transform.strategy.UncommentingStrategy

public data class TransformParameters(
    val adapter: ScannerAdapter.Factory,

    val commenter: CommentingStrategy,
    val uncommenter: UncommentingStrategy,
    val replacer: SwappingStrategy,

    val swaps: Map<String, String> = emptyMap(),
    val constants: Map<String, Boolean> = emptyMap(),
    val dependencies: Map<String, Version> = emptyMap(),

    val replacements: List<Replacement> = emptyList()
) : java.io.Serializable {
    private companion object {
        @java.io.Serial private val serialVersionUID: Long = 0x50E9D2D2FBE78A2B
    }
}