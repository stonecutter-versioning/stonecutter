package dev.kikugie.stitcher.antlr.adapter

import org.antlr.v4.runtime.IntStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.Token

class UnregisteredTokenException(message: String, recognizer: Recognizer<*, *>, input: IntStream, token: Token? = null) : RecognitionException(message, recognizer, input, null) {
    init {
        offendingToken = token
    }
}