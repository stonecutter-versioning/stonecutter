lexer grammar SlashStyleScanner;

@header {
package dev.kikugie.stitcher.antlr.scanner;
}

@members {
public boolean nestMultiLineComments = false;
private byte commentDepth = 0;
}

fragment LINE_BREAK: '\r'|'\n'|'\r\n';
fragment DOUBLE_SLASH: '\\\\';

SLASH_COMMENT_START: '//' -> pushMode(IN_SLASH);
STAR_COMMENT_START: '/*' -> pushMode(IN_STAR);

SINGLE_QUOTE: '\'' -> pushMode(IN_CHAR), skip;
DOUBLE_QUOTE: '"' -> pushMode(IN_STRING), skip;

THE_REST: . -> skip;

mode IN_SLASH;
SLASH_COMMENT_END: LINE_BREAK -> popMode;
SLASH_THE_REST: . -> skip;

mode IN_STAR;
STAR_COMMENT_NEST: '/*' {
    if (nestMultiLineComments) commentDepth++;
} -> skip;
STAR_COMMENT_END: '*/' {
    if (!nestMultiLineComments) popMode();
    else if (commentDepth-- <= 0) popMode();
    else skip();
};
STAR_THE_REST: . -> skip;

mode IN_CHAR;
CH_END: '\'' -> popMode, skip;
CH_ESC_END: (~'\'' | '\\\'' | DOUBLE_SLASH) -> skip;

mode IN_STRING;
STR_END: '"' -> popMode, skip;
STR_ESC_END: (~'"' | '\\"' | DOUBLE_SLASH) -> skip;
