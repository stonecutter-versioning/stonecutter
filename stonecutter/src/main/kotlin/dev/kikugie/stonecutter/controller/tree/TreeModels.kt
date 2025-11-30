@file:UseSerializers(PathSerializer::class)
package dev.kikugie.stonecutter.controller.tree

import dev.kikugie.stonecutter.AnyVersion
import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.StonecutterInternalAPI
import dev.kikugie.stonecutter.build.data.StonecutterBuildData
import dev.kikugie.stonecutter.build.data.StonecutterBuildData.RegexReplacementSpec
import dev.kikugie.stonecutter.build.data.StonecutterBuildData.StringReplacementSpec
import dev.kikugie.stonecutter.controller.flag.StonecutterFlagStorage
import dev.kikugie.stonecutter.data.tree.ProjectBranch
import dev.kikugie.stonecutter.data.tree.ProjectNode
import dev.kikugie.stonecutter.data.tree.ProjectTree
import dev.kikugie.stonecutter.data.version.LenientOperations
import dev.kikugie.semver.data.Version
import dev.kikugie.stitcher.transform.replacement.Replacement
import dev.kikugie.stonecutter.StonecutterPlugin
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.invariantSeparatorsPathString

/**
 * Barebones information about a [NodeModel] for navigation.
 *
 * @property project The subproject name
 * @property version The subproject version
 * @property active Whenever this version is active and assigned to the shared `src/`
 * @property path The absolute project directory
 */
@Serializable @JvmRecord
public data class NodeInfo(
    val project: Identifier,
    val version: AnyVersion = project,
    val active: Boolean = false,
    val path: Path
) {
    internal constructor(node: ProjectNode) :
        this(node.metadata.project, node.metadata.version, node.metadata.isActive, node.location)
}

/**
 * Barebones information about a [BranchModel] for navigation.
 *
 * @property id The branch project name, or an empty string if it's a root branch
 * @property path The absolute branch project directory
 */
@Serializable @JvmRecord
public data class BranchInfo(
    val id: Identifier,
    val path: Path
){
    internal constructor(branch: ProjectBranch) : this(branch.id, branch.location)
}

/**
 * Detailed information about a [ProjectNode].
 *
 * @property project The subproject name
 * @property version The subproject version
 * @property active Whenever this version is active and assigned to the shared `src/`
 * @property branch Branch navigation info
 * @property root [ProjectTree] absolute directory
 * @property parameters Parameters used in file processing
 */
@Serializable @JvmRecord
public data class NodeModel(
    val project: Identifier,
    val version: AnyVersion = project,
    val active: Boolean = false,
    val branch: BranchInfo,
    val root: Path,
    val parameters: ParametersModel,
) {
    @OptIn(StonecutterInternalAPI::class)
    internal constructor(node: ProjectNode, data: StonecutterBuildData) : this(
        node.metadata.project,
        node.metadata.version,
        node.metadata.isActive,
        BranchInfo(node.branch),
        node.tree.location,
        ParametersModel(data)
    )
}

/**
 * Detailed information about a [ProjectBranch].
 *
 * @property id The branch project name, or an empty string if it's a root branch
 * @property root [ProjectTree] absolute directory
 * @property nodes List of node navigation infos
 */
@Serializable @JvmRecord
public data class BranchModel(
    val id: String,
    val root: Path,
    val nodes: List<NodeInfo>,
){
    internal constructor(branch: ProjectBranch) : this(branch.id, branch.location, branch.nodes.map(::NodeInfo))
}

/**
 * Detailed information about a [ProjectTree].
 *
 * @property stonecutter Stonecutter version used to save the model
 * @property vcs Version control reset point
 * @property current The active project name
 * @property branches List of branch navigation infos
 * @property nodes List of all node navigation infos
 * @property flags Non-default Stonecutter flags with stringified values
 */
@Serializable @JvmRecord
public data class TreeModel(
    val stonecutter: String,
    val vcs: Identifier,
    val current: Identifier? = null,
    val branches: List<BranchInfo>,
    val nodes: List<NodeInfo>,
    val flags: Map<String, String>,
){
    internal constructor(tree: ProjectTree, flags: StonecutterFlagStorage) : this(
        StonecutterPlugin.VERSION,
        tree.vcs.project,
        tree.current?.project,
        tree.branches.map(::BranchInfo),
        tree.nodes.map(::NodeInfo),
        flags.serializable
    )
}

/**
 * Detailed information about parameters configured in [StonecutterBuildExtension][dev.kikugie.stonecutter.build.StonecutterBuildExtension].
 *
 * @property constants Name to value constant map
 * @property swaps Name to replacement swap map
 * @property dependencies Name to version dependency map
 * @property replacements All registered replacements
 */
@Serializable @JvmRecord
public data class ParametersModel(
    val constants: Map<Identifier, Boolean> = emptyMap(),
    val swaps: Map<Identifier, String> = emptyMap(),
    val dependencies: Map<Identifier, Version> = emptyMap(),
    val replacements: List<Replacement> = emptyList(),
) : java.io.Serializable {
    internal constructor(data: StonecutterBuildData) : this(
        data.constants.get().toMap(),
        data.swaps.get().toMap(),
        data.dependencies.get().mapValues { (_, it) -> LenientOperations.parse(it) },
        data.stringReplacements.get().map(StringReplacementSpec::build) + data.regexReplacements.get().map(RegexReplacementSpec::build)
    )
}

/**Serializes a [Path] as an absolute path string.*/
public object PathSerializer : KSerializer<Path> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("java.nio.file.Path", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Path): Unit = encoder.encodeString(value.invariantSeparatorsPathString)
    override fun deserialize(decoder: Decoder): Path = Path(decoder.decodeString())
}
