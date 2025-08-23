@file:Suppress("NOTHING_TO_INLINE")

package dev.kikugie.stitcher.data

context(visitor: ExpressionToken.Visitor<T>)
internal inline fun <T> ExpressionToken.acceptThis(): T = accept(visitor)

context(visitor: DefinitionToken.Visitor<T>)
internal inline fun <T> DefinitionToken.acceptThis(): T = accept(visitor)

context(visitor: BlockToken.Visitor<T>)
internal inline fun <T> BlockToken.acceptThis(): T = accept(visitor)

context(visitor: NodeTokenVisitor<T>)
internal inline fun <T> LeafToken.acceptThis(): T = accept(visitor)

context(visitor: NodeTokenVisitor<T>)
internal inline fun <T> PredicateToken.acceptThis(): T = accept(visitor)

internal interface NodeTokenVisitor<T> {
    fun visitLeaf(it: LeafToken): T
    fun visitPredicate(it: PredicateToken): T
}