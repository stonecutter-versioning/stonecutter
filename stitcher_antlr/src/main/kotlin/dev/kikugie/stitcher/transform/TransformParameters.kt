package dev.kikugie.stitcher.transform

import dev.kikugie.semver.data.Version

internal data class TransformParameters(
    val swaps: Map<String, String> = emptyMap(),
    val constants: Map<String, Boolean> = emptyMap(),
    val dependencies: Map<String, Version> = emptyMap(),
)