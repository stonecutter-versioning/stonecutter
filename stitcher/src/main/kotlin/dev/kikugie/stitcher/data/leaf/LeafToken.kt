package dev.kikugie.stitcher.data.leaf

import dev.kikugie.stitcher.data.StitcherToken

internal data class LeafToken(val type: LeafType, override val range: IntRange, override val text: String) : StitcherToken