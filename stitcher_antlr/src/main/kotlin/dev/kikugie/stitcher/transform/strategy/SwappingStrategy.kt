package dev.kikugie.stitcher.transform.strategy

internal fun interface SwappingStrategy {
    /**
     * Replaces provided [scope] with the new [value].
     *
     * The function's purpose is to handle the formatting
     * of the inserted value to match the existing code.
     *
     * Notably, it should:
     * - Preserve the first and the last blank line in a code block.
     * - Maintain the indentation of the existing value.
     */
    fun replace(scope: String, value: String): String
}