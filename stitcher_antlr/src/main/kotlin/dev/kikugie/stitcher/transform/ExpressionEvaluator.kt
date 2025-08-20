package dev.kikugie.stitcher.transform

import dev.kikugie.stitcher.antlr.StitcherParser
import dev.kikugie.stitcher.data.ExpressionToken
import dev.kikugie.stitcher.issue.ProblemCollector
import dev.kikugie.stitcher.issue.ProblemTemplate
import dev.kikugie.stitcher.issue.ReportedBailException
import dev.kikugie.stitcher.issue.at
import dev.kikugie.stitcher.issue.bail
import dev.kikugie.stitcher.issue.checkNotNull
import kotlin.jvm.Throws

private val UNKNOWN_OPERATOR = ProblemTemplate("Unsupported %s operator %s")
private val UNRESOLVED_VALUE = ProblemTemplate("Unresolved %s")

internal class ExpressionEvaluator(val parameters: TransformParameters, val problems: ProblemCollector) : ExpressionToken.Visitor<Boolean> {
    override fun visitGroup(it: ExpressionToken.Group): Boolean = it.body.accept(this)

    @Throws(ReportedBailException::class)
    override fun visitUnary(it: ExpressionToken.Unary): Boolean = when (it.operator.type) {
        StitcherParser.OP_NOT -> !it.operand.accept(this)
        else -> problems.bail(UNKNOWN_OPERATOR.at(it.operator).format("unary", it.operator.typeName))
    }

    @Throws(ReportedBailException::class)
    override fun visitBinary(it: ExpressionToken.Binary): Boolean = when (it.operator.type) {
        StitcherParser.OP_AND -> it.left.accept(this) && it.right.accept(this)
        StitcherParser.OP_OR -> it.left.accept(this) || it.right.accept(this)
        else -> problems.bail(UNKNOWN_OPERATOR.at(it.operator).format("binary", it.operator.typeName))
    }

    @Throws(ReportedBailException::class)
    override fun visitConstant(it: ExpressionToken.Constant): Boolean {
        val value = it.value.text
        return problems.checkNotNull(
            parameters.constants[value], { UNRESOLVED_VALUE.at(it.value).format("constant") })
    }

    @Throws(ReportedBailException::class)
    override fun visitAssignment(it: ExpressionToken.Assignment): Boolean {
        val value = it.target?.text.orEmpty()
        val target = problems.checkNotNull(
            parameters.dependencies[value], { UNRESOLVED_VALUE.at(it.target ?: it.predicates.first()).format("dependency") })
        return it.predicates.all { it.comparator(target, it.version) }
    }
}