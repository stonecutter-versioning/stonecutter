package dev.kikugie.stitcher.api

import dev.kikugie.semver.data.Version
import dev.kikugie.stitcher.transform.TransformParameters

fun parameters(action: TransformParametersBuilder.() -> Unit): TransformParameters =
    TransformParametersBuilder().apply(action).build()

@DslMarker
annotation class ParametersDSL

@ParametersDSL
class TransformParametersBuilder @PublishedApi internal constructor(){
    val swaps: MutableMap<String, String> = mutableMapOf()
    val constants: MutableMap<String, Boolean> = mutableMapOf()
    val dependencies: MutableMap<String, String> = mutableMapOf()

    @PublishedApi
    internal fun build() = TransformParameters(
        swaps.toMap(),
        constants.toMap(),
        dependencies.mapValues { Version.parse(it.value).getOrThrow() }
    )
}

