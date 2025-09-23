package dev.kikugie.stitcher.transform

import dev.kikugie.semver.data.Version
import dev.kikugie.stitcher.parse.adapter.ScannerAdapter
import dev.kikugie.stitcher.transform.replacement.Replacement
import dev.kikugie.stitcher.transform.strategy.CommentingStrategy
import dev.kikugie.stitcher.transform.strategy.SwappingStrategy
import dev.kikugie.stitcher.transform.strategy.UncommentingStrategy

internal data class TransformParameters(
    val adapter: ScannerAdapter.Factory,

    val commenter: CommentingStrategy,
    val uncommenter: UncommentingStrategy,
    val replacer: SwappingStrategy,

    val swaps: Map<String, String> = emptyMap(),
    val constants: Map<String, Boolean> = emptyMap(),
    val dependencies: Map<String, Version> = emptyMap(),

    val replacements: List<Replacement> = emptyList()
)