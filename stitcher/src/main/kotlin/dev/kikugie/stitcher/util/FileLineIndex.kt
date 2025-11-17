package dev.kikugie.stitcher.util

import dev.kikugie.stitcher.antlr.Lines
import dev.kikugie.stitcher.issue.ProblemLocation
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

public class FileLineIndex private constructor(private val lines: IntArray) {
    public constructor(stream: CharStream) : this (buildLineIndexes(stream))
    private val max: Int = lines.lastIndex

    public fun locate(index: Int): ProblemLocation {
        val lineIndex = findLineIndex(index)
        val charOffset = index - lines[lineIndex]
        return ProblemLocation(lineIndex + 1, charOffset + 1)
    }

    @Throws(IndexOutOfBoundsException::class)
    public fun indexOf(location: ProblemLocation): Int {
        check(!location.isUndefined) { "Unable to index an undefined location" }
        return indexOf(location.line, location.column - 1)
    }

    @Throws(IndexOutOfBoundsException::class)
    public fun indexOf(line: Int, offset: Int): Int =
        lines[line - 1] + offset

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