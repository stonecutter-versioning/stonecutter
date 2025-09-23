package dev.kikugie.stitcher.data

internal enum class DefinitionType {
    SCOPED_OPENER, LINE_OPENER, WORD_OPENER,
    SCOPED_EXTENSION, LINE_EXTENSION, WORD_EXTENSION,
    CLOSER, INDEPENDENT;

    val isScoped: Boolean get() = when(this) {
        SCOPED_OPENER, SCOPED_EXTENSION -> true
        else -> false
    }

    val isOpen: Boolean get() = when(this) {
        LINE_OPENER, WORD_OPENER, LINE_EXTENSION, WORD_EXTENSION -> true
        else -> false
    }

    val isExtension: Boolean get() = when(this) {
        SCOPED_EXTENSION, LINE_EXTENSION, WORD_EXTENSION, CLOSER -> true
        else -> false
    }

    val isEmpty: Boolean get() = when(this) {
        CLOSER, INDEPENDENT -> true
        else -> false
    }
}