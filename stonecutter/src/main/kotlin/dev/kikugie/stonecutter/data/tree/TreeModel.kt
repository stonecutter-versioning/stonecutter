@file: UseSerializers(PathSerializer::class)

package dev.kikugie.stonecutter.data.tree

import dev.kikugie.stonecutter.StonecutterAPI
import dev.kikugie.stonecutter.data.parameters.BuildParameters
import dev.kikugie.stitcher.util.PathSerializer
import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.data.StonecutterProject
import dev.kikugie.stonecutter.data.parameters.GlobalParameters
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.Json
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists
import kotlin.io.path.readText
import kotlin.io.path.writeText

private fun <T> save(location: Path, model: T, serializer: KSerializer<T>): Result<Unit> = location.runCatching {
    val json = Json.encodeToString(serializer, model)
    parent.createDirectories()
    writeText(
        json,
        Charsets.UTF_8,
        StandardOpenOption.WRITE,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING
    )
}

private fun <T> load(location: Path, serializer: KSerializer<T>): Result<T> = location.runCatching {
    if (location.notExists()) throw NoSuchFileException(location.toFile())
    val text = readText(Charsets.UTF_8)
    Json.decodeFromString(serializer, text)
}

@Serializable
public data class NodeInfo(
    val project: Identifier,
    val version: String = project,
    val active: Boolean = false,
    val path: Path,
) {
    public constructor(metadata: StonecutterProject, path: Path)
        : this(metadata.project, metadata.version, metadata.isActive, path)
}

@Serializable
public data class BranchInfo(
    val id: String,
    val path: Path,
)

/**
 * Represents serialised information about a versioned subproject.
 * This is stored in:
 * - `build/stonecutter-cache/node.json` for the active node.
 * - `versions/{project}/build/stonecutter-cache/node.json` for each node in the branch.
 */
@Serializable
public data class NodeModel(
    val project: Identifier,
    val version: String = project,
    val active: Boolean = false,
    val branch: BranchInfo,
    val root: Path,
    val parameters: BuildParameters
) {
    public companion object {
        /**Literally `node.json`.*/
        public const val FILENAME: String = "node.json"

        /**Loads the `node.json` file as [NodeModel] from the given [directory].*/
        @JvmStatic @StonecutterAPI
        public fun load(directory: Path): Result<NodeModel> =
            load(directory.resolve(FILENAME), serializer())
    }

    internal fun save(directory: Path): Result<Unit> =
        save(directory.resolve(FILENAME), this, serializer())
}

@Serializable
public data class BranchModel(
    val id: String,
    val root: Path,
    val nodes: List<NodeInfo>,
) {
    public companion object {
        /**Literally `branch.json`.*/
        public const val FILENAME: String = "branch.json"

        /**Loads the `branch.json` file as [BranchModel] from the given [directory].*/
        @JvmStatic @StonecutterAPI
        public fun load(directory: Path): Result<BranchModel> =
            load(directory.resolve(FILENAME), serializer())
    }

    internal fun save(directory: Path): Result<Unit> =
        save(directory.resolve(FILENAME), this, serializer())
}

@Serializable
public data class TreeModel(
    val stonecutter: String,
    val vcs: Identifier,
    val current: Identifier,
    val branches: List<BranchInfo>,
    val nodes: List<NodeInfo>,
    val flags: GlobalParameters,
) {
    public companion object {
        /**Literally `tree.json`.*/
        public const val FILENAME: String = "tree.json"

        /**Loads the `tree.json` file as [TreeModel] from the given [directory].*/
        @JvmStatic @StonecutterAPI
        public fun load(directory: Path): Result<TreeModel> =
            load(directory.resolve(FILENAME), serializer())
    }

    internal fun save(directory: Path): Result<Unit> =
        save(directory.resolve(FILENAME), this, serializer())
}