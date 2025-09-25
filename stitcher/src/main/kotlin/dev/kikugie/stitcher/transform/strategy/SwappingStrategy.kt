package dev.kikugie.stitcher.transform.strategy

/**
 * Represents a strategy for replacing a specific scope of text with a new value.
 */
public fun interface SwappingStrategy : java.io.Serializable {
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
    public fun replace(scope: String, value: String): String
}