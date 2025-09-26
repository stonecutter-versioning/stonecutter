package dev.kikugie.stonecutter.util

internal fun Char.isEnglishLetter() =
    this in 'a'..'z' || this in 'A'..'Z'

internal fun Char.isIdentifierStart() = when (this) {
    '_' -> true
    else -> isEnglishLetter()
}

internal fun Char.isIdentifierPart() = when (this) {
    '_', '-' -> true
    else -> isEnglishLetter() || isDigit()
}

/**
 * Checks if the string matches the [dev.kikugie.stonecutter.Identifier]
 * requirements.
 */
public fun isIdentifier(str: String): Boolean = str.isNotEmpty()
    && str.first().isIdentifierStart()
    && str.all(Char::isIdentifierPart)
