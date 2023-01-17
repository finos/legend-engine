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
    '{' PROJECT ':'  (BRACE_OPEN projectExpression? ( ',' projectExpression )* BRACE_CLOSE) '}';

// TODO: handle taking in $cond as an arguement to $project pipeline https://www.mongodb.com/docs/manual/reference/operator/aggregation/cond/
// TODO: handle taking in $substr https://www.mongodb.com/docs/manual/reference/operator/aggregation/substr/
// TODO: handle taking in $substrBytes https://www.mongodb.com/docs/manual/reference/operator/aggregation/substrBytes/#mongodb-expression-exp.-substrBytes
projectExpression: STRING ':' ( projectionValue | ( BRACE_OPEN projectExpression? ( ',' projectExpression )* BRACE_CLOSE ) );

projectionValue: projectionBooleanValue | projectionComputedFieldValue;

// https://www.mongodb.com/docs/manual/reference/operator/aggregation/project/#include-computed-fields
projectionComputedFieldValue: STRING_WITH_DOLLAR;

projectionBooleanValue: '0' | '1' | 'true' | 'false';


// Aggregation Operator expressions
// https://www.mongodb.com/docs/manual/reference/operator/aggregation/


// Logical Query Operator Expressions

//https://www.mongodb.com/docs/manual/reference/operator/query-logical/

logicalOperatorExpression: andAggregationExpression | orAggregationExpression;

andAggregationExpression: BRACE_OPEN AND ':' (BRACKET_OPEN queryExpression? ( ',' queryExpression )* BRACKET_CLOSE ) BRACE_CLOSE;
orAggregationExpression: BRACE_OPEN OR ':' (BRACKET_OPEN queryExpression? ( ',' queryExpression )* BRACKET_CLOSE ) BRACE_CLOSE;

// TODO: add others..



//Document mongo.queryFilters.QueryFilter Filter

//https://www.mongodb.com/docs/manual/core/document/#std-label-document-query-filter
//{
//  <field1>: <value1>,
//  <field2>: { <operator>: <value> },
//  ...
//}

queryExpression: BRACE_OPEN expression ( ',' expression )* BRACE_CLOSE;

expression: STRING ':' expressionValue | COMPARISON_QUERY_OPERATOR ':' complexExpressionValue;
expressionValue: STRING | NUMBER | complexExpressionValue;
complexExpressionValue: complexObjectExpressionValue | complexArrayExpressionValue;


complexObjectExpressionValue: BRACE_OPEN COMPARISON_QUERY_OPERATOR ':' expressionValue BRACE_CLOSE;
complexArrayExpressionValue: BRACKET_OPEN complexExpressionValue ( ',' complexExpressionValue )* BRACKET_CLOSE;



// LEXER

// Comparison Query Operators
// https://www.mongodb.com/docs/manual/reference/operator/query-comparison/
COMPARISON_QUERY_OPERATOR: EQ | NE | GT | GTE;

EQ : '"' '$eq' '"' |  '$eq';
NE : '"' '$ne' '"' |  '$ne';
GT: '"' '$gt' '"'  |  '$gt';
GTE: '"' '$gte' '"' |  '$gte';

//... add others


// Logical Query Operators
// https://www.mongodb.com/docs/manual/reference/operator/query-logical/

AND : '"' '$and' '"' | '$and';
OR : '"' '$or' '"' |  '$or';

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
