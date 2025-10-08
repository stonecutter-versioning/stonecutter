@file:OptIn(ExperimentalContracts::class)

package dev.kikugie.stitcher.util

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.text.StringBuilder

private val IDENTIFIER_REGEX: Regex = Regex("[_a-zA-Z][_\\-a-zA-Z0-9]*")

public fun String.isValidIdentifier(): Boolean = matches(IDENTIFIER_REGEX)

internal inline fun buildString(content: String, action: StringBuilder.() -> Unit): String {
    contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
    return StringBuilder(content).apply(action).toString()
}