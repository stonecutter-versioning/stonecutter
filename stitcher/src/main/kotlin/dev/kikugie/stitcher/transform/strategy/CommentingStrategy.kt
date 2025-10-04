package dev.kikugie.stitcher.transform.strategy

/**
 * Defines a strategy for adding comments to a given scope of text.
 */
public fun interface CommentingStrategy {
    /**
     * Adds comments to the specified [scope].
     *
     * The implementation should take nested comments and indentation
     * of the scope into consideration.
     */
    public fun comment(scope: String): String
}