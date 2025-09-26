package dev.kikugie.stitcher.util

import dev.kikugie.stitcher.antlr.Lines
import dev.kikugie.stitcher.issue.ProblemLocation
import dev.kikugie.stitcher.issue.ProblemSink
import org.antlr.v4.runtime.CharStream

private fun buildLineIndexes(stream: CharStream): IntArray = try {
    val lexer = Lines(stream)
    val indexes = ArrayList<Int>()
    for (token in lexer.asSequence())
        indexes += token.startIndex
    indexes.toIntArray()
} finally {
    stream.seek(0)
}

internal class FileLineIndex private constructor(private val lines: IntArray) {
    constructor(stream: CharStream) : this (buildLineIndexes(stream))
    private val max: Int = lines.lastIndex

    fun locate(index: Int, sink: ProblemSink): ProblemLocation {
        val lineIndex = findLineIndex(index)
        val charOffset = index - lines[lineIndex]
        return ProblemLocation(lineIndex + 1, charOffset, sink)
    }

    private tailrec fun findLineIndex(index: Int, min: Int = 0, max: Int = this.max): Int {
        if (min == max) return min // Converged onto one position
        val middle = min + (max - min) / 2
        val low = lines[middle]
        val high = if (middle < this.max) lines[middle + 1] else Int.MAX_VALUE

        return if (index in low..<high) middle
        else if (index < low) findLineIndex(index, min, middle)
        else findLineIndex(index, middle + 1, max)
    }
}