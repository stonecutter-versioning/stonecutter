grammar StitcherLight;

@header {
package dev.kikugie.stitcher.antlr;
}

/* PARSER */
definition
    : CONTENT EOF
    | REPL_MARK CONTENT EOF
    | SWAP_MARK SCOPE_CLOSE? CONTENT* (SCOPE_OPEN | SCOPE_WORD)? EOF
    | COND_MARK SCOPE_CLOSE? CONTENT* (SCOPE_OPEN | SCOPE_WORD)? EOF
    ;

/* LEXER */
fragment ESC_SLASH: '\\\\';
fragment ESC_STAR: '\\*';
fragment ESC_TICK: '\\\'';

COND_MARK: '?';
SWAP_MARK: '$';
REPL_MARK: '~';

SCOPE_CLOSE: '}';
SCOPE_OPEN: '{';
SCOPE_WORD: '>>';

QUOTED: '\'' (ESC_SLASH | ESC_TICK | ~[\\'] )* '\'' -> type(CONTENT);
COMMENT: '*' (ESC_SLASH | ESC_STAR | ~[*\\])* '*' -> skip;
WHITESPACE: [ \t] -> skip;
CONTENT: .;