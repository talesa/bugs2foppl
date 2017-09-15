grammar bugs;

// PARSER

input:   /* includes empty */
| modelStatement
// | varStatement modelStatement
// | dataStatement modelStatement
// | varStatement dataStatement modelStatement
;

var:
  name=ID                     # varID
| name=ID '[' rangeList ']'   # varIndexed
// Workaround for I and T to be recognized as names of variables as well
| name='T'                    # varID
| name='T' '[' rangeList ']'  # varIndexed
| name='I'                    # varID
| name='I' '[' rangeList ']'  # varIndexed
;

stochasticRelation:
  var '~' distribution
| var '~' distribution truncated
| var '~' distribution interval
;

interval: 'I' '(' expression ','  expression ')'
| 'I' '(' ',' expression ')'
| 'I' '(' expression ',' ')'
| 'I' '(' ',' ')'
;

truncated: 'T' '(' expression ','  expression ')'
| 'T' '(' ',' expression ')'
| 'T' '(' expression ',' ')'
| 'T' '(' ',' ')'
;

varStatement: VAR declarationList
| VAR declarationList ';'
;

declarationList: nodeDeclaration
| declarationList ',' nodeDeclaration
;

nodeDeclaration: ID
| ID '[' dimensionsList ']'
;

dimensionsList: expression
| dimensionsList ',' expression
;

dataStatement: DATA '{' relationList '}';

modelStatement: MODEL '{' relationList '}';

rangeList: rangeElement
| rangeList ',' rangeElement
;

rangeElement:
| expression
;

forLoop: counter relations;

counter: FOR '(' var IN rangeElement ')';

assignment: '=' | '<-';

/*
 The link function is given using an S-style replacement function
 notation.  We need to turn this round so the inverse link
 function is applied to the RHS of the deterministic relation
*/
deterministicRelation:
  var assignment expression                 # deterministicRelation1
| ID '(' var ')' assignment expression      # deterministicRelationLink
;

expressionList:
  expression                        //# expressionList1
| expressionList ',' expression     //# expressionList2
//| expression ',' expressionList     //# expressionList2
;

expression:
  var                                       # varExpression
|<assoc=right> expression '^' expression    # exponentiation
| expression '*' expression                 # arithmetic
| expression '/' expression                 # arithmetic
| expression '+' expression                 # arithmetic
| expression '-' expression                 # arithmetic
| '-' expression                            # negation
| DOUBLE                                    # atom
| LENGTH '(' var ')'                        # lenExpression
| DIM '(' var ')'                           # dimExpression
| ID '(' expressionList ')'                 # function
| expression ':' expression                 # rangeExpression
| expression SPECIAL expression             # specialExpression
| '(' expression ')'                        # parenExpression
;

// negation: '-' expression;

//BUGS has a dflat() distribution with no parameters
distribution:
  ID '(' ')'
| ID '(' expressionList ')'
;

relations: '{' relationList '}' ;

// TODO changes here, may break sth
relationList:
  relation                  //# relationList1
| relationList relation     //# relationList2
//| relation relationList     # relationList2
;

relation:
  stochasticRelation ';'
| stochasticRelation
| deterministicRelation ';'
| deterministicRelation
| forLoop
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

ID: [a-zA-Z] [a-zA-Z0-9._]*;

EXPONENT:   [eE][+-]?[0-9]+;
DOUBLE: ([0-9]+) EXPONENT?
| ([0-9]+'.'[0-9]*) EXPONENT?
| ('.'[0-9]+) EXPONENT?;


