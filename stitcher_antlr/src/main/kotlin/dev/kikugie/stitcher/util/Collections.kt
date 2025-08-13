package dev.kikugie.stitcher.util

internal fun <T> Array<out T?>.firstNotNull(): T? {
    for (it in this)
        if (it != null) return it
    return null
}

internal fun <T> Array<out T?>.lastNotNull(): T? {
    for (i in size - 1 downTo 0)
        get(i)?.let { return it }
    return null
}