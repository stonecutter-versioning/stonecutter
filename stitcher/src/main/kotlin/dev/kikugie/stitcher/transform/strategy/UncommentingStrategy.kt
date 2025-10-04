package dev.kikugie.stitcher.transform.strategy

/**
 * Defines a strategy for uncommenting a given scope of text.
 */
public fun interface UncommentingStrategy {
    /**
     * Removes comments from the specified [scope].
     *
     * The [scope] is the text between [opener] and [closer] excluding the latter two.
     * ```kt
     * // hello!
     * |  |     ^ closer "\n"
     * |  ^ scope " hello!"
     * ^ opener "//"
     * ```
     * ```kt
     * /*
     * hello!
     * */
     * - opener "/*"
     * - scope "\nhello!\n"
     * - closer "*/"
     * ```
     *
     * The uncommenting strategy should **only** operate on the [scope],
     * using [opener] and [closer] only as helpers.
     * *(For example, `'\n' in closer || '\r' in closer` could be used to detect a line comment,
     * instructing the implementation to add a line break after the scope)*
     *
     * If the comment syntax doesn't need any complex processing,
     * [UncommentingStrategy.SIMPLE] can be used, or [UncommentingStrategy.LINED] to handle single-line comments.
     */
    public fun uncomment(scope: String, opener: String, closer: String): String

    public companion object {
        /**
         * Performs no uncommenting logic, returning the scope directly.
         */
        @JvmField public val SIMPLE: UncommentingStrategy = UncommentingStrategy { it, _ , _ ->
            it
        }

        /**
         * If the comment closer contains a line break, appends it to the scope;
         * otherwise returns the scope directly.
         */
        @JvmField public val LINED: UncommentingStrategy = UncommentingStrategy { it, _, cl ->
            if ('\r' in cl || '\n' in cl) it + cl else it
        }
    }
}