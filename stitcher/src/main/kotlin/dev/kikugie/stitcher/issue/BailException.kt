package dev.kikugie.stitcher.issue

/**
 * Represents an error that should be caught but not reported to avoid duplicate entries.
 */
public class BailException : RuntimeException()