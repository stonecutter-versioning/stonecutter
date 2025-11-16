package dev.kikugie.stitcher.issue

/**
 * Represents the problem location in the processed file.
 * @property line Line in the file, starting at 1
 * @property column Column in the [line], starting at 1
 * @property isUndefined Whenever the associated issue applies to the whole file
 */
@JvmInline
public value class ProblemLocation private constructor(private val packed: Long) {
    public constructor(line: Int, column: Int) : this((line.toLong() and 0xFFFFFFFFL) or (column.toLong() shl 32))

    public val line: Int get() = (packed and 0xFFFFFFFFL).toInt()
    public val column: Int get() = (packed ushr 32 and 0xFFFFFFFFL).toInt()
    public val isUndefined: Boolean get() = packed == -1L

    public companion object {
        public val UNDEFINED: ProblemLocation = ProblemLocation(-1L)
    }
}