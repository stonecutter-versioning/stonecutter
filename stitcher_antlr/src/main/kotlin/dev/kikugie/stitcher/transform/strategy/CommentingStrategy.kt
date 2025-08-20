package dev.kikugie.stitcher.transform.strategy

internal fun interface CommentingStrategy {
    fun comment(scope: String): String
}