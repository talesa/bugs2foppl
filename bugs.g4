grammar bugs;


// PARSER

input:   /* empty */
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

assignment: '='
| '<-'
;

/*
 The link function is given using an S-style replacement function
 notation.  We need to turn this round so the inverse link
 function is applied to the RHS of the deterministic relation
*/
determ_relation: var assignment expression
| ID '(' var ')' assignment expression;

//product: expression '*' expression
////  This creates a shift-reduce conflict because in the expression
////  A*B*C, (A*B) is a valid expression. By default, bison shifts,
////  which is what we want. The warning is suppressed with the %expect
////  declaration (See also sum: below).
//| product '*' expression
//;

//sum: expression '+' expression
////  This creates a shift-reduce conflict. By default, bison shifts,
////  which is what we want. The warning is suppressed with the %expect
////  declaration (See also product: above).
//| sum '+' expression
//;


expression: var
| expression '^'<assoc=right> expression
//| product
| expression '*' expression // product
| expression '/' expression
//| sum
| expression '+' expression // sum
| expression '-' expression
| negation //%prec NEG
| DOUBLE
| LENGTH '(' var ')'
| DIM '(' var ')'
| ID '(' expression_list ')'
| expression ':' expression
| expression SPECIAL expression
| '(' expression ')'
//| expression GT expression
//| expression GE expression
//| expression LT expression
//| expression LE expression
//| expression EQ expression
//| expression NE expression
//| expression AND expression
//| expression OR expression
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

distribution: ID '(' ')'
//BUGS has a dflat() distribution with no parameters
| ID '(' expression_list ')'
;

//func: ID '(';


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

//BRACKET: [ \t]* [(];

VAR: 'var';
DATA: 'data';
MODEL: 'model';

//IN: 'in' ;
//SEMICOLON: ';';
//COMMA: ',';
//COLON: ':';
//LSQBR: '[';
//RSQBR: ']';
//LBR: '(';
//RBR: ')';
//LCUBR: '{';
//RCUBR: '}';
//LE: '<=';
//LT: '<';
//GE: '>=';
//GT: '>';
//AND: '&&';
//OR: '||';
//NE: '!=';
//NOT: '!';
//EQ: '==';
//EQUALS: '=';
//TILDE: '~';
//ARROW: '<-';
//PLUS: '+';
//MINUS: '-';
//POWER: '^' | '**';
//TIMES: '*';
//SLASH: '/';




//LENGTH: 'length' BRACKET;
//DIM: 'dim' BRACKET;
//FOR: 'for' BRACKET;
//IN: 'in' BRACKET | 'in';



//LENGTH: 'length' {_input.LA(1) == '('}?;
//DIM: 'dim' {_input.LA(1) == '('}?;
//FOR: 'for' {_input.LA(1) == '('}?;
//IN: 'in' {_input.LA(1) == '('}? | 'in';

LENGTH: 'length';
DIM: 'dim';
FOR: 'for';
IN: 'in';

//
//"length"/{BRACKET}      return LENGTH;
//"dim"/{BRACKET}         return DIM;
//"for"/{BRACKET}        	return FOR;
//"in"/{BRACKET}		return IN;


/* Special operators, e.g. %*% for matrix multiplication */
SPECIAL: '%'+[^% \t\r\n]*'%';

EXPONENT:   [eE][+-]?[0-9]+;
DOUBLE: ([0-9]+) EXPONENT?
| ([0-9]+'.'[0-9]*) EXPONENT?
| ('.'[0-9]+) EXPONENT?;

ID: [a-zA-Z] [a-zA-Z0-9._]*;

//FUNC: ([a-zA-Z]+[a-zA-Z0-9\.\_]*) BRACKET ;
//FUNC: ([a-zA-Z]+[a-zA-Z0-9._]*) BRACKET ;
//FUNC: [a-zA-Z]+[a-zA-Z0-9]*;//[a-zA-Z0-9]* ;
//ID: [a-zA-Z]+s[a-zA-Z0-9\.\_]*;
//ID: [a-zA-Z]+[a-zA-Z0-9._]*;

//FUNC: [a-zA-Z]+[a-zA-Z0-9._]* {_input.LA(1) == '('}?;

//T: 'T' {_input.LA(1) == '('}?;
//I: 'I' {_input.LA(1) == '('}?;


//T: 'T' BRACKET;
//I: 'I' BRACKET;
//T: 'T';
//I: 'I';
//"T"/{BRACKET}           return 'T';
//"I"/{BRACKET}           return 'I';
