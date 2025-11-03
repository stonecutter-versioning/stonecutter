package dev.kikugie.stitcher.parser.layout

/**
 * Response from the scope on the top of the stack when pushing a new block to it.
 */
internal sealed interface BlockAcceptResult {
    /**
     * The scope is full - pop the stack and try on the next one.
     */
    data object Rejected : BlockAcceptResult

    /**
     * Accepted and can potentially accept more.
     */
    data object ConsumedOpen : BlockAcceptResult

    /**
     * Accepted, but should pop the stack.
     */
    data object ConsumedFinal : BlockAcceptResult

    /**
     * Accepted a slice - pop the stack and push [remainder] to the next one.
     */
    data class ConsumedPartial(val remainder: BlockBuilder) : BlockAcceptResult
}