package dev.kikugie.stitcher.transform.strategy

internal fun interface SwappingStrategy {
    fun replace(scope: String, value: String): String
}