package dev.kikugie.stonecutter.data

import dev.kikugie.stonecutter.AnyVersion
import dev.kikugie.stonecutter.Identifier
import kotlinx.serialization.Serializable

/**
 * Essential Stonecutter project metadata.
 *
 * @property project The Gradle project name in `versions/` directory
 * @property version The stringified project version, used for conditional evaluation
 */
@Serializable
public data class StonecutterProject(val project: Identifier, val version: AnyVersion) {
    /**Wrapped parsed [version] used for comparisons.*/
    public val parsed: ParsedVersion by lazy { ParsedVersion(version) }

    /**Whenever this project is assigned as active, with the shared `src/` directory linked to it.*/
    public var isActive: Boolean = false
        internal set
}