package dev.kikugie.stitcher.transform.strategy

/**
 * Defines a strategy for adding comments to a given scope of text.
 */
public fun interface CommentingStrategy : java.io.Serializable {
    /**
     * Adds comments to the specified [scope].
     *
     * The implementation should take nested comments and indentation
     * of the scope into consideration. The [full] parameter indicates
     * if the entire block needs to be commented, or the end can be trimmed.
     */
    public fun comment(scope: String, open: Boolean): String
}