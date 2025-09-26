lexer grammar Lines;

@header {
package dev.kikugie.stitcher.antlr;
}

fragment CONTENT: ~([\r\n]);
fragment LINE_BREAK: '\r'|'\n'|'\r\n';

LINE: CONTENT* LINE_BREAK
    | CONTENT+
    ;
