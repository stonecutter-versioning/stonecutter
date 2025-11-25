package dev.kikugie.stitcher.transform.replacement

/**
 * Exception thrown to indicate an issue encountered during replacement validation.
 * @property issue The problem explanation
 * @property sample Replacement state representation
 */
public class ReplacementException(public val issue: String, public val sample: String) : RuntimeException("$issue: '$sample'")