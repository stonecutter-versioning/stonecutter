package dev.kikugie.stonecutter.data

import dev.kikugie.stonecutter.AnyVersion
import dev.kikugie.stonecutter.Identifier
import kotlinx.serialization.Serializable

@Serializable
public data class StonecutterProject(val project: Identifier, val version: AnyVersion) {
    public val parsed: ParsedVersion by lazy { ParsedVersion(version) }

    public var isActive: Boolean = false
        internal set
}