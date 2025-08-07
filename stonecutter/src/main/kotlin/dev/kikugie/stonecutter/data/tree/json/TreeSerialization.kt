@file:OptIn(ExperimentalSerializationApi::class, ExperimentalContracts::class)
@file:Suppress("SENSELESS_COMPARISON")

package dev.kikugie.stonecutter.data.tree.json

import dev.kikugie.stonecutter.Identifier
import dev.kikugie.stonecutter.Version
import dev.kikugie.stonecutter.data.tree.builder.BranchBuilder
import dev.kikugie.stonecutter.data.tree.builder.TreeBuilder
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.reflect.full.functions

private fun kotlinCompilerShittingItselfWorkaround(decoder: JsonDecoder): JsonObject {
    return decoder.decodeJsonElement().jsonObject

//    val function = checkNotNull(decoder::class.functions.find { it.name == "decodeJsonElement" })
//    { "No 'decodeJsonElement' function found" }
//
//    val result = function.call(decoder)
//    return checkNotNull(result as? JsonObject)
//    { "Result is ${if (result == null) null else result::class.qualifiedName}" }
}

private inline fun <T> serCheckNotNull(value: T?, message: () -> String): T {
    contract {
        returns() implies (value != null)
    }
    if (value != null) return value
    else throw SerializationException(message())
}

@Serializable(with = SerializedTree.TreeJsonSerializer::class)
internal data class SerializedTree(
    val vcs: Identifier? = null,
    val kotlinController: Boolean? = null,
    val schemes: List<TreeScheme>
) {
    fun applyTo(builder: TreeBuilder) {
        builder.vcsVersion.set(vcs)
        builder.kotlinController.set(kotlinController)
        for (it in schemes) it.applyTo(builder)
    }

    internal object TreeJsonSerializer : KSerializer<SerializedTree> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor(this::class.simpleName!!)

        override fun serialize(encoder: Encoder, value: SerializedTree) {
            throw UnsupportedOperationException()
        }

        override fun deserialize(decoder: Decoder): SerializedTree {
            val element = kotlinCompilerShittingItselfWorkaround(decoder as JsonDecoder)
            val vcs = element["vcs"]?.jsonPrimitive?.content
            val controller = element["kotlinController"]?.jsonPrimitive?.boolean ?: true

            val byBranches = (element["branches"])?.let { deserializeByBranches(decoder, it) }
            val byVersions = (element["versions"])?.let { deserializeByVersions(decoder, it) }
            return SerializedTree(vcs, controller, listOfNotNull(byVersions, byBranches))
        }

        private fun deserializeByVersions(decoder: JsonDecoder, element: JsonElement): TreeScheme = when (element) {
            is JsonObject -> Json.decodeFromJsonElement(TreeScheme.Inverted.serializer(), element)
            is JsonArray -> Json.decodeFromJsonElement(TreeScheme.Plain.serializer(), element)
            else -> throw SerializationException("Unable to decode tree from ${element::class.simpleName}")
        }

        private fun deserializeByBranches(decoder: JsonDecoder, element: JsonElement): TreeScheme =
            Json.decodeFromJsonElement(TreeScheme.Branched.serializer(), element)
    }
}

@Serializable(with = SerializedVersion.VersionJsonSerializer::class)
internal data class SerializedVersion(
    val project: Identifier,
    val version: Version = project,
    val buildscript: String? = null,
) {
    internal object PrimitiveJsonSerializer : KSerializer<SerializedVersion> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(this::class.simpleName!!, PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: SerializedVersion) {
            throw UnsupportedOperationException()
        }

        override fun deserialize(decoder: Decoder): SerializedVersion {
            val components = decoder.decodeString().split(':', limit = 3)
            val project = serCheckNotNull(components.firstOrNull()) { "Missing project component" }
            val version = components.getOrNull(1) ?: project
            val buildscript = components.getOrNull(2)
            return SerializedVersion(project, version, buildscript)
        }
    }

    internal object ObjectJsonSerializer : KSerializer<SerializedVersion> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor(this::class.simpleName!!) {
            element<Identifier>("project")
            element<Version>("version", isOptional = true)
            element("buildscript", serialDescriptor<String>().nullable, isOptional = true)
        }

        override fun serialize(encoder: Encoder, value: SerializedVersion) {
            throw UnsupportedOperationException()
        }

        override fun deserialize(decoder: Decoder): SerializedVersion {
            val element = kotlinCompilerShittingItselfWorkaround(decoder as JsonDecoder)
            val project = serCheckNotNull(element["project"]) { "Missing project component" }.jsonPrimitive.content
            val version = element["version"]?.jsonPrimitive?.content ?: project
            val buildscript = element["buildscript"]?.jsonPrimitive?.content
            return SerializedVersion(project, version, buildscript)
        }
    }

    internal object VersionJsonSerializer : JsonContentPolymorphicSerializer<SerializedVersion>(SerializedVersion::class) {
        override fun selectDeserializer(element: JsonElement): DeserializationStrategy<SerializedVersion> = when (element) {
            is JsonPrimitive -> PrimitiveJsonSerializer
            is JsonObject -> ObjectJsonSerializer
            else -> throw SerializationException("Unable to decode a version from ${element::class.simpleName}")
        }
    }
}

@Serializable
internal sealed interface TreeScheme {
    fun applyTo(builder: TreeBuilder)

    @Serializable @JvmInline
    value class Plain(val versions: List<SerializedVersion>) : TreeScheme {
        override fun applyTo(builder: TreeBuilder) = versions.applyTo(builder)
    }

    @Serializable @JvmInline
    value class Branched(val branches: Map<Identifier, @Serializable(with = NestedVersionsSerializer::class) List<SerializedVersion>>) : TreeScheme {
        override fun applyTo(builder: TreeBuilder) = branches.forEach { (name, versions) ->
            builder.branch(name) { versions.applyTo(this) }
        }
    }

    @Serializable @JvmInline
    value class Inverted(
        val versions: Map<
            @Serializable(with = SerializedVersion.PrimitiveJsonSerializer::class) SerializedVersion,
            @Serializable(with = NestedBranchesSerializer::class) List<Identifier>>
    ) : TreeScheme {
        override fun applyTo(builder: TreeBuilder) = Branched(invertBranches())
            .applyTo(builder)

        private fun invertBranches(): Map<String, MutableList<SerializedVersion>> = buildMap {
            for ((version, branches) in versions) for (branch in branches)
                getOrPut(branch, ::mutableListOf) += version
        }
    }

    private object NestedVersionsSerializer : JsonTransformingSerializer<List<SerializedVersion>>(ListSerializer(SerializedVersion.serializer())) {
        override fun transformDeserialize(element: JsonElement): JsonElement = when (element) {
            is JsonObject -> element["versions"]?.jsonArray ?: JsonArray(emptyList())
            else -> element
        }
    }

    private object NestedBranchesSerializer : JsonTransformingSerializer<List<Identifier>>(ListSerializer(Identifier.serializer())) {
        override fun transformDeserialize(element: JsonElement): JsonElement = when (element) {
            is JsonObject -> element["branches"]?.jsonArray ?: JsonArray(emptyList())
            else -> element
        }
    }

    private companion object {
        fun List<SerializedVersion>.applyTo(builder: BranchBuilder) {
            for ((path, versions) in groupBy(SerializedVersion::buildscript))
                builder.versions(versions.map { it.project to it.version })
                    .apply { if (path != null) buildscript(path) }
        }
    }
}

