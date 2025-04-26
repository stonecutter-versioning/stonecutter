package dev.kikugie.stitcher.lexer

import dev.kikugie.semver.VersionParser
import dev.kikugie.semver.VersionParsingException
import dev.kikugie.stitcher.data.token.NullType
import dev.kikugie.stitcher.data.token.StitcherTokenType.*
import dev.kikugie.stitcher.data.token.TokenType
import dev.kikugie.stitcher.data.token.WhitespaceType
import dev.kikugie.stitcher.util.StringUtil.countStart

/**
 * Class responsible for matching and slicing lexical tokens in a given [input].
 */
class TokenMatcher(private val input: CharSequence) {
    companion object {
        /**
         * Checks if the character is a valid identifier character.
         *
         * A valid identifier character can be an uppercase or lowercase letter,
         * a digit, or one of the special characters: '_', '-', '+', '.'.
         *
         * This is used by the lexer, as well as by the Gradle module to validate
         * subproject names.
         */
        fun Char.isValidIdentifier() = when (this) {
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k',
            'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
            'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G',
            'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R',
            'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2',
            '3', '4', '5', '6', '7', '8', '9', '_', '-', '+', '.' -> true
            else -> false
        }
    }

    /**
     * Matches a token at the specified [offset] in the [input] sequence.
     * If no token matches, it returns a slice containing all characters
     * after the [offset] assigned a [NullType].
     */
    fun match(offset: Int): LexSlice = matchInternal(offset, input[offset])
        ?: slice(offset..<input.length, NullType)

    private fun matchInternal(offset: Int, ch: Char): LexSlice? = when (ch) {
        '<', '=', '~', '^' -> matchPredicate(offset, true)
        ' ', '\t' -> matchWhitespace(offset)
        '(' -> slice(offset, GROUP_OPEN)
        ')' -> slice(offset, GROUP_CLOSE)
        '{' -> slice(offset, SCOPE_OPEN)
        '}' -> slice(offset, SCOPE_CLOSE)
        ':' -> slice(offset, ASSIGN)
        '!' -> slice(offset, NEGATE)
        '&' -> matchString(offset, "&&", AND)
        '|' -> matchString(offset, "||", OR)
        'i' -> matchString(offset, "if", IF)
            ?: matchIdentifier(offset)

        'e' -> matchString(offset, "else", ELSE)
            ?: matchString(offset, "elif", ELIF)
            ?: matchIdentifier(offset)

        '>' -> matchString(offset, ">>", EXPECT_WORD)
            ?: matchPredicate(offset, true)
        else -> matchAny(offset)
    }

    private fun matchWhitespace(offset: Int): LexSlice =
        slice(offset..<offset + input.countStart(offset + 1, ' ', '\t') + 1, WhitespaceType)

    private fun matchString(offset: Int, pattern: String, type: TokenType): LexSlice? = when {
        offset + pattern.length > input.length -> null
        input.substring(offset, offset + pattern.length) != pattern -> null
        else -> slice(offset..<offset + pattern.length, type)
    }

    private fun matchAny(offset: Int): LexSlice? =
        matchPredicate(offset, false) ?: matchIdentifier(offset)

    private fun matchIdentifier(offset: Int): LexSlice {
        val count = input.countStart(offset) { it.isValidIdentifier() }
        return slice(offset..<offset + count, IDENTIFIER)
    }

    private fun matchPredicate(offset: Int, lenient: Boolean): LexSlice? = try {
        val end = if (lenient) VersionParser.parsePredicateLenient(input, offset).end
        else VersionParser.parsePredicate(input, offset).end
        slice(offset..<end, PREDICATE)
    } catch (e: VersionParsingException) {
        null
    }

    private fun slice(range: IntRange, type: TokenType) =
        LexSlice(type, range, input)

    private fun slice(index: Int, type: TokenType) =
        LexSlice(type, index..index, input)
}