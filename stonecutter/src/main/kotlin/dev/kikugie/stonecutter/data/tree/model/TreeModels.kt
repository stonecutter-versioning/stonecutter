@file:UseSerializers(PathSerializer::class)
package dev.kikugie.stonecutter.data.tree.model

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.Version
import dev.kikugie.stonecutter.build.param.StonecutterBuildData
import dev.kikugie.stonecutter.data.StonecutterProject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.invariantSeparatorsPathString

@Serializable @JvmInline
public value class ActiveInfo private constructor(private val value: String?) {
    public companion object {
        public fun empty(): ActiveInfo = ActiveInfo(null)
        public fun of(identifier: Identifier): ActiveInfo = ActiveInfo("name@$identifier")
        public fun of(path: Path): ActiveInfo = ActiveInfo("path@${path.invariantSeparatorsPathString}")
    }

    public val isPath: Boolean get() = value?.startsWith("path@") == true
    public val isIdentifier: Boolean get() = value?.startsWith("name@") == true

    public fun asPathOrNull(): Path? = value?.split { type, str ->
        if (type == "path") Path(str) else null
    }

    public fun asIdentifierOrNull(): Identifier? = value?.split { type, str ->
        str.takeIf { type == "name" }
    }

    private inline fun <T> String.split(action: (String, String) -> T): T {
        val index = indexOf('@')
        return action(take(index), substring(index + 1))
    }
}

@Serializable
public data class NodeInfo(
    val project: Identifier,
    val version: Version = project,
    val active: Boolean = false,
    val path: Path
) {
    public constructor(metadata: StonecutterProject, path: Path)
        : this(metadata.project, metadata.version, metadata.isActive, path)
}

@Serializable
public data class BranchInfo(
    val id: String,
    val path: Path
)

@Serializable
public data class NodeModel(
    val project: Identifier,
    val version: Version = project,
    val active: Boolean = false,
    val branch: BranchInfo,
    val root: Path,
    val parameters: StonecutterBuildData,
) {
    public constructor(metadata: StonecutterProject, branch: BranchInfo, root: Path, parameters: StonecutterBuildData)
        : this(metadata.project, metadata.version, metadata.isActive, branch, root, parameters)
}

@Serializable
public data class BranchModel(
    val id: String,
    val root: Path,
    val nodes: List<NodeInfo>,
)

/**
 * Represents the Stonecutter tree model serialized in `build/stonecutter-cache/tree.json`.
 *
 * @property stonecutter The used Stonecutter Gradle plugin version as set in [StonecutterPlugin.VERSION][dev.kikugie.stonecutter.StonecutterPlugin.VERSION].
 * @property vcs The VCS version project name.
 * @property current The resolved active version.
 * @property currentProvider The source of the active version.
 * @property branches References to the branch model locations.
 * @property nodes References to node model locations.
 * @property flags Overridden Stonecutter flags.
 */
@Serializable
public data class TreeModel(
    val stonecutter: String,
    val vcs: Identifier,
    val current: Identifier? = null,
    val branches: List<BranchInfo>,
    val nodes: List<NodeInfo>,
    val flags: Map<String, String>,
    @SerialName("current_provider")
    val currentProvider: ActiveInfo,
)