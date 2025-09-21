package dev.kikugie.stitcher.parse.adapter

import dev.kikugie.stitcher.data.LeafToken
import dev.kikugie.stitcher.util.AntlrToken
import org.antlr.v4.runtime.tree.ParseTree

internal interface AntlrTokenConverter {
    operator fun invoke(tree: ParseTree): LeafToken
    operator fun invoke(token: AntlrToken): LeafToken
    operator fun invoke(vararg aggregate: AntlrToken): LeafToken
}