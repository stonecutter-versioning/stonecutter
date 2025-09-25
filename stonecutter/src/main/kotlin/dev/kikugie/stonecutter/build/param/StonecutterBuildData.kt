package dev.kikugie.stonecutter.build.param

import dev.kikugie.semver.data.Version
import dev.kikugie.stitcher.transform.replacement.Replacement
import dev.kikugie.stonecutter.Identifier
import kotlinx.serialization.Serializable

/**
 * Represents the build data serialized in `node.json`.
 */
@Serializable
public data class StonecutterBuildData(
    public val constants: Map<Identifier, Boolean>,
    public val swaps: Map<Identifier, String>,
    public val dependencies: Map<Identifier, Version>,
    public val replacements: List<Replacement>,
)
