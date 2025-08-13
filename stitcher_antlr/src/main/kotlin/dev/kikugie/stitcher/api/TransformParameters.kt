package dev.kikugie.stitcher.api

import dev.kikugie.semver.data.Version

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

data class TransformParameters(
    val swaps: Map<String, String> = emptyMap(),
    val constants: Map<String, Boolean> = emptyMap(),
    val dependencies: Map<String, Version> = emptyMap(),
)