package dev.kikugie.stitcher.transform.strategy

internal fun interface UncommentingStrategy {
    fun uncomment(scope: String): String
}