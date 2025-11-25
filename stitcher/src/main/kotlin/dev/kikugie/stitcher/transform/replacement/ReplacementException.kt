package dev.kikugie.stitcher.transform.replacement

internal class ReplacementException(val issue: String, val sample: String) : RuntimeException("$issue: '$sample'")