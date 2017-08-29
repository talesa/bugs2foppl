grammar R_data;

//prog:   (expr (';'|NL) | NL)* EOF;

prog:   expr* EOF;

/*
expr_or_assign
    :   expr ('<-'|'='|'<<-') expr_or_assign
    |   expr
    ;
*/

// '[[' follows R's yacc grammar
expr:   expr '[[' sublist ']' ']'                             # expression
    |   expr '[' sublist ']'                                  # expression
    |   expr ('::'|':::') expr                                # expression
    |   expr ('$'|'@') expr                                   # expression
    |   <assoc=right> expr '^' expr                           # expression
    |   ('-'|'+') expr                                        # expression
    |   expr ':' expr                                         # expression
    // anything wrappedin %: '%' .* '%'
    |   expr USER_OP expr                                     # expression
    |   expr ('*'|'/') expr                                   # expression
    |   expr ('+'|'-') expr                                   # expression
    |   expr ('>'|'>='|'<'|'<='|'=='|'!=') expr               # expression
    |   '!' expr                                              # expression
    |   expr ('&'|'&&') expr                                  # expression
    |   expr ('|'|'||') expr                                  # expression
    |   '~' expr                                              # expression
    |   expr '~' expr                                         # expression
    |   expr ('<-'|'<<-'|'='|'->'|'->>'|':=') expr            # expression
    |   'function' '(' formlist? ')' expr                     # functionDefinition
    |   expr '(' sublist ')'                                  # functionCall
    // compound statement
    |   '{' exprlist '}'                                      # expression
    |   'if' '(' expr ')' expr                                # expression
    |   'if' '(' expr ')' expr 'else' expr                    # expression
    |   'for' '(' ID 'in' expr ')' expr                       # expression
    |   'while' '(' expr ')' expr                             # expression
    |   'repeat' expr                                         # expression
    // get help on expr, usually string or ID
    |   '?' expr                                              # expression
    |   'next'                                                # expression
    |   'break'                                               # expression
    |   '(' expr ')'                                          # expression
    |   ID                                                    # expression
    |   STRING                                                # expression
    |   HEX                                                   # expression
    |   INT                                                   # expression
    |   FLOAT                                                 # expression
    |   COMPLEX                                               # expression
    |   'NULL'                                                # expression
    |   'NA'                                                  # expression
    |   'Inf'                                                 # expression
    |   'NaN'                                                 # expression
    |   'TRUE'                                                # expression
    |   'FALSE'                                               # expression
    ;

exprlist
    :   expr ((';'|NL) expr?)*
    |
    ;

formlist : form (',' form)* ;

form:   ID
    |   ID '=' expr
    |   '...'
    ;

sublist : sub (',' sub)* ;

sub :   expr
    |   ID '='
    |   ID '=' expr
    |   STRING '='
    |   STRING '=' expr
    |   'NULL' '='
    |   'NULL' '=' expr
    |   '...'
    |
    ;

HEX :   '0' ('x'|'X') HEXDIGIT+ [Ll]? ;

INT :   DIGIT+ [Ll]? ;

fragment
HEXDIGIT : ('0'..'9'|'a'..'f'|'A'..'F') ;

FLOAT:  DIGIT+ '.' DIGIT* EXP? [Ll]?
    |   DIGIT+ EXP? [Ll]?
    |   '.' DIGIT+ EXP? [Ll]?
    ;
fragment
DIGIT:  '0'..'9' ;
fragment
EXP :   ('E' | 'e') ('+' | '-')? INT ;

COMPLEX
    :   INT 'i'
    |   FLOAT 'i'
    ;

STRING
    :   '"' ( ESC | ~[\\"] )*? '"'
    |   '\'' ( ESC | ~[\\'] )*? '\''
    |   '`' ( ESC | ~[\\'] )*? '`'
    ;

fragment
ESC :   '\\' [abtnfrv"'\\]
    |   UNICODE_ESCAPE
    |   HEX_ESCAPE
    |   OCTAL_ESCAPE
    ;

fragment
UNICODE_ESCAPE
    :   '\\' 'u' HEXDIGIT HEXDIGIT HEXDIGIT HEXDIGIT
    |   '\\' 'u' '{' HEXDIGIT HEXDIGIT HEXDIGIT HEXDIGIT '}'
    ;

fragment
OCTAL_ESCAPE
    :   '\\' [0-3] [0-7] [0-7]
    |   '\\' [0-7] [0-7]
    |   '\\' [0-7]
    ;

fragment
HEX_ESCAPE
    :   '\\' HEXDIGIT HEXDIGIT?
    ;

ID  :   '.' (LETTER|'_'|'.') (LETTER|DIGIT|'_'|'.')*
    |   LETTER (LETTER|DIGIT|'_'|'.')*
    ;

fragment LETTER  : [a-zA-Z] ;

USER_OP :   '%' .*? '%' ;

COMMENT :   '#' .*? '\r'? '\n' -> type(NL) ;

// Match both UNIX and Windows newlines
NL      :   '\r'? '\n' -> skip ;

WS      :   [ \t\u000C]+ -> skip ;
