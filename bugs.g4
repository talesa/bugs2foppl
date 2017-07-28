grammar bugs;


// PARSER

input:   /* includes empty */
| model_stmt
| var_stmt model_stmt
| data_stmt model_stmt
| var_stmt data_stmt model_stmt
;

var: ID
| ID '[' range_list ']'
;

stoch_relation:	var '~' distribution
//| var '~' distribution truncated
//| var '~' distribution interval
;

//interval: 'I' '(' expression ','  expression ')'
//| 'I' '(' ',' expression ')'
//| 'I' '(' expression ',' ')'
//| 'I' '(' ',' ')'
//;
//
//truncated: 'T' '(' expression ','  expression ')'
//| 'T' '(' ',' expression ')'
//| 'T' '(' expression ',' ')'
//| 'T' '(' ',' ')'
//;

var_stmt: VAR dec_list
| VAR dec_list ';'
;

dec_list: node_dec
| dec_list ',' node_dec
;

node_dec: ID
| ID '[' dim_list ']'
;

dim_list: expression
| dim_list ',' expression
;

data_stmt: DATA '{' relation_list '}';

model_stmt: MODEL '{' relation_list '}';

for_loop: counter relations;

counter: FOR '(' ID IN range_element ')';

assignment: '=' | '<-';

/*
 The link function is given using an S-style replacement function
 notation.  We need to turn this round so the inverse link
 function is applied to the RHS of the deterministic relation
*/
determ_relation: var assignment expression
| ID '(' var ')' assignment expression;


expression: var
| expression '^'<assoc=right> expression
| expression '*' expression
| expression '/' expression
| expression '+' expression
| expression '-' expression
| negation
| DOUBLE
| LENGTH '(' var ')'
| DIM '(' var ')'
| ID '(' expression_list ')'
| expression ':' expression
| expression SPECIAL expression
| '(' expression ')'
;

negation: '-' expression;

expression_list: expression
| expression_list ',' expression
;

range_list: range_element
| range_list ',' range_element
;

range_element:
| expression
;

//BUGS has a dflat() distribution with no parameters
distribution: ID '(' ')'
| ID '(' expression_list ')'
;


relations: '{' relation_list '}' ;

relation_list:	relation
| relation_list relation
;

relation: stoch_relation
| determ_relation
| for_loop
| stoch_relation ';'
| determ_relation ';'
;

// LEXER

WHITESPACE: [ \t\r\n\f]+ -> skip ; /* Eat whitespace */

/* Eat comments */
ONE_LINE_COMMENT: '#'.*?[\n] -> skip;
COMMENT: '/*' .*? '*/' -> skip;

VAR: 'var';
DATA: 'data';
MODEL: 'model';

LENGTH: 'length';
DIM: 'dim';
FOR: 'for';
IN: 'in';

/* Special operators, e.g. %*% for matrix multiplication */
SPECIAL: '%'+[^% \t\r\n]*'%';

EXPONENT:   [eE][+-]?[0-9]+;
DOUBLE: ([0-9]+) EXPONENT?
| ([0-9]+'.'[0-9]*) EXPONENT?
| ('.'[0-9]+) EXPONENT?;

ID: [a-zA-Z] [a-zA-Z0-9._]*;