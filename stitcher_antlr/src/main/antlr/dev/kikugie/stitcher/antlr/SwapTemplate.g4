lexer grammar SwapTemplate;

@header {
package dev.kikugie.stitcher.antlr;
}

TEMPLATE: '$' [0-9]+;
CONTENT: ('\\\\' | '\\$' | ~[^]) -> skip;