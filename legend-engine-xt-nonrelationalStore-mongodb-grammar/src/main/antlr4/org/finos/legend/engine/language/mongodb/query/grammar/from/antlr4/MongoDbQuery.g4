grammar MongoDbQuery;


/**
 * Parser Rules
 */

// some grammar examples https://raw.githubusercontent.com/mongodb-js/mongodb-language-model/master/grammar.pegjs
// examples from mongo java driver, in legend project search for com.mongodb.client.model.Aggregates class and $match there

databaseCommand:
    command
    EOF
;

command:
BRACE_OPEN
    AGGREGATE COLON STRING COMMA
    PIPELINE COLON pipelines COMMA
    CURSOR COLON '{' '}'
BRACE_CLOSE;

pipelines: '[' aggregationPipelineStage? ( ',' aggregationPipelineStage)* ']';

// Aggregation Pipeline Stages
//https://www.mongodb.com/docs/manual/reference/operator/aggregation-pipeline/

aggregationPipelineStage: matchStage | projectStage;

// https://www.mongodb.com/docs/manual/reference/operator/aggregation/match/#mongodb-pipeline-pipe.-match
matchStage:
    '{' MATCH ':' ( BRACE_OPEN BRACE_CLOSE | queryExpression | logicalOperatorExpression) '}';
// TODO: handle taking in $exp as part of $match value


//https://www.mongodb.com/docs/manual/reference/operator/aggregation/project/
projectStage:
    '{' PROJECT ':'  (BRACE_OPEN BRACE_CLOSE | projectFilterExpression ) '}';

// TODO: handle taking in $cond as an arguement to $project pipeline https://www.mongodb.com/docs/manual/reference/operator/aggregation/cond/
// TODO: handle taking in $substr https://www.mongodb.com/docs/manual/reference/operator/aggregation/substr/
// TODO: handle taking in $substrBytes https://www.mongodb.com/docs/manual/reference/operator/aggregation/substrBytes/#mongodb-expression-exp.-substrBytes
// equivalent of queryExpression
projectFilterExpression: BRACE_OPEN projectFilter ( ',' projectFilter )* BRACE_CLOSE;
// equivalent of expression
projectFilter: STRING ':' projectFilterValue;
// equivalent of expressionValue
projectFilterValue: NUMBER | BOOLEAN | projectComputedFieldValue | projectFilterExpression;
// https://www.mongodb.com/docs/manual/reference/operator/aggregation/project/#include-computed-fields
projectComputedFieldValue: STRING_WITH_DOLLAR;



// Aggregation Operator expressions
// https://www.mongodb.com/docs/manual/reference/operator/aggregation/


// Logical Query Operator Expressions

//https://www.mongodb.com/docs/manual/reference/operator/query-logical/

logicalOperatorExpression: BRACE_OPEN LOGICAL_QUERY_OPERATOR ':' logicalOperatorExpressionValueArray BRACE_CLOSE;
logicalOperatorExpressionValueArray: BRACKET_OPEN logicalOperatorExpressionValue ( ',' logicalOperatorExpressionValue )* BRACKET_CLOSE;
logicalOperatorExpressionValue: queryExpression | comparisonOperatorExpression | value;

// TODO: add others..



//Document mongo.queryFilters.QueryFilter Filter

//https://www.mongodb.com/docs/manual/core/document/#std-label-document-query-filter
//{
//  <field1>: <value1>,
//  <field2>: { <operator>: <value> },
//  ...
//}

queryExpression: BRACE_OPEN expression ( ',' expression )* BRACE_CLOSE;

expression: STRING ':' expressionValue;
expressionValue: value | comparisonOperatorExpression | logicalOperatorExpression;

comparisonOperatorExpression: BRACE_OPEN COMPARISON_QUERY_OPERATOR ':' value BRACE_CLOSE;


obj
   : '{' pair (',' pair)* '}'
   | '{' '}'
   ;

pair
   : STRING ':' value
   ;

arr
   : '[' value (',' value)* ']'
   | '[' ']'
   ;

value
   : STRING
   | NUMBER
   | obj
   | arr
   | BOOLEAN
   | NULL
   ;

// LEXER

// Comparison Query Operators
// https://www.mongodb.com/docs/manual/reference/operator/query-comparison/
COMPARISON_QUERY_OPERATOR: EQ | NE | GT | GTE | LT | LTE | IN | NIN;

EQ : '"' '$eq' '"' |  '$eq';
GT: '"' '$gt' '"'  |  '$gt';
GTE: '"' '$gte' '"' |  '$gte';
LT: '"' '$lt' '"'  |  '$lt';
LTE: '"' '$lte' '"' |  '$lte';
NE : '"' '$ne' '"' |  '$ne';
IN : '"' '$in' '"' |  '$in';
NIN : '"' '$nin' '"' |  '$nin';



//... add others


// Logical Query Operators
// https://www.mongodb.com/docs/manual/reference/operator/query-logical/

LOGICAL_QUERY_OPERATOR: AND | OR | NOR | NOT;

AND : '"' '$and' '"' | '$and';
OR : '"' '$or' '"' |  '$or';
NOR : '"' '$nor' '"' |  '$nor';
NOT : '"' '$not' '"' |  '$not';

//... add others


BRACE_OPEN : '{';
BRACE_CLOSE : '}';

BRACKET_OPEN : '[';
BRACKET_CLOSE : ']';

AGGREGATE : '"' 'aggregate' '"' |  'aggregate';
PIPELINE : '"' 'pipeline' '"' |  'pipeline';
CURSOR : '"' 'cursor' '"' |  'cursor';
MATCH : '"' '$match' '"' |  '$match';
PROJECT : '"' '$project' '"' |  '$project';


// to handle // https://www.mongodb.com/docs/manual/reference/operator/aggregation/project/#include-computed-fields
STRING_WITH_DOLLAR // TODO: is this the correct way of having precedence for strings with "$field1.field11" ??
   : '"$' (ESC | SAFECODEPOINT)* '"'
   ;

STRING
   : '"' (ESC | SAFECODEPOINT)* '"'
   ;

NUMBER
   : '-'? INT ('.' [0-9] +)? EXP?
   ;

NULL: 'null';

BOOLEAN: TRUE | FALSE;

TRUE: 'true';
FALSE: 'false';

fragment INT
   : '0' | [1-9] [0-9]*
   ;

// no leading zeros

fragment EXP
   : [Ee] [+\-]? INT
   ;

fragment ESC
   : '\\' (["\\/bfnrt] | UNICODE)
   ;
fragment UNICODE
   : 'u' HEX HEX HEX HEX
   ;
fragment HEX
   : [0-9a-fA-F]
   ;
fragment SAFECODEPOINT
   : ~ ["\\\u0000-\u001F]
   ;


WORD: [_A-Za-z] [_0-9A-Za-z]*;
COLON: ':';

WS: [ \t\n\r]+ -> skip;

COMMA: ',';
