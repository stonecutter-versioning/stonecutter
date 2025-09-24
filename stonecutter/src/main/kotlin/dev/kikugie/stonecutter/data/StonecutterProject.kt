package dev.kikugie.stonecutter.data

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.Version
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@StonecutterAPI @Serializable
public data class StonecutterProject(
    /**The name of this project's directory, as in `versions/${project}`.*/
    public val project: Identifier,
    /**The assigned version of this project, used in comment evaluation.*/
    public val version: Version,
) {
    @Transient
    public var isActive: Boolean = false
        private set

    /**Represents the project as '[project]:[version]'.*/
    override fun toString(): String = "$project:$version"

    /**
     * Provides access to setting the [isActive] state to third-party tools.
     * It **must not** be used on instances used in project configuration,
     * as it will result in undefined behaviours.
     */
    @StonecutterInternalAPI
    public fun overrideActiveState(active: Boolean): StonecutterProject =
        apply { isActive = active }
}
