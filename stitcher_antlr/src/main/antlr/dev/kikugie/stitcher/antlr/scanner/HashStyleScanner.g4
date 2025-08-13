lexer grammar HashStyleScanner;

@header {
package dev.kikugie.stitcher.antlr.scanner;
}

fragment LINE_BREAK: '\r'|'\n'|'\r\n';
fragment DOUBLE_SLASH: '\\\\';

HASH_COMMENT_START: '#' -> pushMode(IN_HASH);

SINGLE_QUOTE: '\'' -> pushMode(IN_CHAR), skip;
DOUBLE_QUOTE: '"' -> pushMode(IN_STRING), skip;

THE_REST: . -> skip;

mode IN_HASH;
SLASH_COMMENT_END: LINE_BREAK -> popMode;

mode IN_CHAR;
CH_END: '\'' -> popMode, skip;
CH_ESC_END: '\\\'' -> skip;
CH_ESC_SLASH: DOUBLE_SLASH -> skip;
CH_THE_REST: ~'\'' -> skip;

mode IN_STRING;
STR_END: '"' -> popMode, skip;
STR_ESC_END: '\\"' -> skip;
STR_ESC_SLASH: DOUBLE_SLASH -> skip;
STR_THE_REST: ~'"' -> skip;
