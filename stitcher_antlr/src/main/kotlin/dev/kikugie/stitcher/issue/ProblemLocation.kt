package dev.kikugie.stitcher.issue

import kotlin.properties.Delegates

internal sealed interface ProblemLocation {
    val line: Int
    val offset: Int

    data class Direct(override val line: Int, override val offset: Int) : ProblemLocation

    data class Lazy(val index: Int) : ProblemLocation {
        override var line by Delegates.notNull<Int>()
        override var offset by Delegates.notNull<Int>()
    }
}
