package dev.kikugie.stonecutter.util

import dev.kikugie.stitcher.util.isValidIdentifier

/**
 * Checks if the string matches the [dev.kikugie.stonecutter.Identifier]
 * requirements.
 */
// Can't be bothered to update it with the new function everywhere
public fun isIdentifier(str: String): Boolean = str.isValidIdentifier()
