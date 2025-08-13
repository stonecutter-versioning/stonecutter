grammar Layout;

@header {
package dev.kikugie.stitcher.antlr;
}

tokens {
    CONTENT,
    COMMENT_OPEN,
    COMMENT_BODY,
    REPLACEMENT,
    SWAP_OPENER,
    SWAP_FREE_OPENER,
    SWAP_CLOSER,
    CONDITION_OPENER,
    CONDITION_FREE_OPENER,
    CONDITION_EXTENSION,
    CONDITION_FREE_EXTENSION,
    CONDITION_CLOSER,
    COMMENT_CLOSE
}

file: block* EOF;

block
    : content
    | comment
    | condition
    | replacement
    | swap
    ;

content: CONTENT;
comment
    : COMMENT_OPEN COMMENT_BODY COMMENT_CLOSE;

replacement
    : COMMENT_OPEN REPLACEMENT COMMENT_CLOSE;

swap
    : swapFreeOpener block
    | swapOpener block+ swapCloser
    ;

swapOpener
    : COMMENT_OPEN SWAP_OPENER COMMENT_CLOSE;

swapFreeOpener
    : COMMENT_OPEN SWAP_FREE_OPENER COMMENT_CLOSE;

swapCloser
    : COMMENT_OPEN SWAP_CLOSER COMMENT_CLOSE;

condition
    : conditionFreeOpener block
    | conditionOpener block+ extension
    ;

extension
    : conditionCloser
    | conditionFreeExtension block
    | conditionExtension block+ extension
    ;

conditionOpener
    : COMMENT_OPEN CONDITION_OPENER COMMENT_CLOSE;

conditionFreeOpener
    : COMMENT_OPEN CONDITION_FREE_OPENER COMMENT_CLOSE;

conditionExtension
    : COMMENT_OPEN CONDITION_EXTENSION COMMENT_CLOSE;

conditionFreeExtension
    : COMMENT_OPEN CONDITION_FREE_EXTENSION COMMENT_CLOSE;

conditionCloser
    : COMMENT_OPEN CONDITION_CLOSER COMMENT_CLOSE;

