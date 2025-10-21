grammar Stitcher;

@header {
package dev.kikugie.stitcher.antlr;
}

/* PARSER */
definition
    : COND_MARK condition EOF   # conditionDefinition
    | SWAP_MARK swap EOF        # swapDefinition
    | REPL_MARK replacement EOF # replacementDefinition
    ;

scopeOpener
    : SCOPE_OPEN                  # closedScopeOpener
    | SCOPE_WORD (PLUS? literal)? # wordScopeOpener
    ;

replacement: IDENTIFIER;

swap
    : IDENTIFIER swapArguments? scopeOpener? # openerSwap
    | SCOPE_CLOSE                            # closerSwap
    ;

swapArguments: literal+;

condition
    : SUGAR_IF? conditionExpression scopeOpener?                                                                                    # openerCondition
    | SCOPE_CLOSE (((SUGAR_ELSE SUGAR_IF | SUGAR_ELIF | SUGAR_ELSE)? conditionExpression scopeOpener?) | (SUGAR_ELSE scopeOpener?)) # extensionCondition
    | SCOPE_CLOSE                                                                                                                   # closerCondition
    ;

conditionExpression
    : conditionExpression op=(OP_AND | OP_OR) conditionExpression # binaryExpression
    | OP_NOT conditionExpression                                  # unaryExpression
    | LEFT_BRACE conditionExpression RIGHT_BRACE                  # groupExpression
    | IDENTIFIER                                                  # constantExpression
    | (IDENTIFIER OP_ASSIGN)? versionPredicate+                   # assignmentExpression
    ;

versionPredicate
    : (stringComparator | semanticComparator)? semanticVersion # semanticPredicate
    | stringComparator stringVersion?                         # stringPredicate
    ;

semanticVersion
    : versionCore (DASH preRelease)? (PLUS buildMetadata)?
    ;

stringVersion
    : IDENTIFIER
    ;

semanticComparator
    : REPL_MARK
    | COMP_MAJOR
    ;

stringComparator
    : COMP_EQUAL
    | COMP_NEQUAL
    | COMP_MORE
    | COMP_GMORE
    | COMP_LESS
    | COMP_GLESS
    ;

versionCore: NUMERIC (DOT NUMERIC)*;
preRelease: metadata (DOT metadata)*;
buildMetadata: metadata (DOT metadata)*;

metadata: NUMERIC | IDENTIFIER;
literal: IDENTIFIER | QUOTED;

/* LEXER */
fragment NUMBER: '0'|[1-9][0-9]*;
fragment IDENTIFIER_START: [_a-zA-Z];
fragment IDENTIFIER_PART: [_\-a-zA-Z0-9];

fragment ESC_SLASH: '\\\\';
fragment ESC_STAR: '\\*';
fragment ESC_TICK: '\\\'';

COND_MARK: '?';
SWAP_MARK: '$';
REPL_MARK: '~';

DOT: '.';
DASH: '-';
PLUS: '+';

LEFT_BRACE: '(';
RIGHT_BRACE: ')';

SCOPE_CLOSE: '}';
SCOPE_OPEN: '{';
SCOPE_WORD: '>>';

SUGAR_IF: 'if';
SUGAR_ELSE: 'else';
SUGAR_ELIF: 'elif';

COMP_MAJOR: '^';
COMP_EQUAL: '=';
COMP_NEQUAL: '!=';
COMP_MORE: '>';
COMP_GMORE: '>=';
COMP_LESS: '<';
COMP_GLESS: '<=';

OP_ASSIGN: ':';
OP_NOT: '!';
OP_AND: '&&';
OP_OR: '||';

NUMERIC: NUMBER;
IDENTIFIER: IDENTIFIER_START IDENTIFIER_PART*;
QUOTED: '\'' (ESC_SLASH | ESC_TICK | ~[\\'] )* '\'';
COMMENT: '*' (ESC_SLASH | ESC_STAR | ~[\\*])* '*' -> channel(HIDDEN);
WHITESPACE: [ \t]+ -> channel(HIDDEN);
