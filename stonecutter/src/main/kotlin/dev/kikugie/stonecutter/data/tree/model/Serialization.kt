package dev.kikugie.stonecutter.data.tree.model

import dev.kikugie.stonecutter.controller.ext.FlagContainer
import dev.kikugie.stonecutter.controller.flag.StonecutterFlag
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.invariantSeparatorsPathString

public object PathSerializer : KSerializer<Path> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("java.nio.file.Path", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Path): Unit = encoder.encodeString(value.invariantSeparatorsPathString)
    override fun deserialize(decoder: Decoder): Path = Path(decoder.decodeString())
}

@OptIn(ExperimentalSerializationApi::class)
public object FlagContainerJsonSerializer : KSerializer<FlagContainer> {
    override val descriptor: SerialDescriptor get() = delegate.descriptor
    private val delegate = MapSerializer(String.serializer(), String.serializer())

    override fun serialize(encoder: Encoder, value: FlagContainer): Unit =
        TODO()
//        delegate.serialize(encoder, (value).ma.mapValues { (_, value) -> value.toString() })

    override fun deserialize(decoder: Decoder): FlagContainer =
        TODO()
//        delegate.deserialize(decoder).mapValues { (key, value) -> StonecutterFlag.named(key).fromString(value) }
//            .let(::FlagContainerImpl)
}