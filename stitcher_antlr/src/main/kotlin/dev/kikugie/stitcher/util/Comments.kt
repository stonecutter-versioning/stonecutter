@file:OptIn(ExperimentalContracts::class)

package dev.kikugie.stitcher.util

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.text.StringBuilder

internal inline fun buildString(content: String, action: StringBuilder.() -> Unit): String {
    contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
    return StringBuilder(content).apply(action).toString()
}