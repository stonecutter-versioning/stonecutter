@file:Suppress("NOTHING_TO_INLINE")

package dev.kikugie.stitcher.util

@DslMarker
annotation class TerminationDSL

@TerminationDSL @Deprecated("Should use problem collectors")
inline fun unsupported(reason: String): Nothing {
    throw UnsupportedOperationException(reason)
}

@TerminationDSL @Deprecated("Should use problem collectors")
inline fun missing(message: String): Nothing {
    throw NoSuchElementException(message)
}

@TerminationDSL @Deprecated("Should use problem collectors")
inline fun <T : Any> verify(value: T?, message: () -> String): T {
    return value ?: missing(message())
}

@TerminationDSL @Deprecated("Should use problem collectors")
inline fun checkNot(value: Boolean, message: () -> String) =
    check(!value, message)