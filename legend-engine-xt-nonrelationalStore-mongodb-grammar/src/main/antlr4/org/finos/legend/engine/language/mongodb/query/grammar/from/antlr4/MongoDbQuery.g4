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
OPEN_CURLY
    AGGREGATE COLON stringValue COMMA
    PIPELINE COLON pipelines
CLOSE_CURLY;

pipelines: '[' ']'
    | stageList
    ;

stageList: '[' matchStage ( ',' matchStage)* ']'; // one item or several separate by comma


matchStage:
    '{' MATCH ':' ( OPEN_CURLY CLOSE_CURLY | queryFilter) '}';


//Document mongo.queryFilters.QueryFilter Filter

//https://www.mongodb.com/docs/manual/core/document/#std-label-document-query-filter
//{
//  <field1>: <value1>,
//  <field2>: { <operator>: <value> },
//  ...
//}

queryFilter: OPEN_CURLY filterExpression ( ',' filterExpression )* CLOSE_CURLY;

filterExpression: simpleFilterExpression | filterExpressionWithOperator;

//{
//  <field1>: <value1>,
simpleFilterExpression: WORD ':' STRING;
//  <field2>: { <operator>: <value> },
//  ...
//}
filterExpressionWithOperator: WORD ':' OPEN_CURLY QUERY_SELECTOR ':' STRING CLOSE_CURLY;


//https://spec.graphql.org/June2018/#sec-String-Value
stringValue : STRING;

// mongo.queryFilters.QueryFilter Operation Commands
// https://www.mongodb.com/docs/manual/reference/command/nav-crud/


// Database Commands

// Aggregration Commands
//https://www.mongodb.com/docs/manual/reference/operator/aggregation/interface/

// Aggregate
//https://www.mongodb.com/docs/manual/reference/command/aggregate/#mongodb-dbcommand-dbcmd.aggregate


// mongo.queryFilters.QueryFilter Operators

// Comparison

//mongo.queryFilters.QueryFilter Selectors
//https://www.mongodb.com/docs/manual/reference/operator/query/

// LEXER


QUERY_SELECTOR: EQ;

EQ : '$eq';


OPEN_CURLY : '{';
CLOSE_CURLY : '}';

AGGREGATE : 'aggregate';
PIPELINE : 'pipeline';
CURSOR : 'cursor';


//Aggregation pipeline stages
//https://www.mongodb.com/docs/manual/reference/operator/aggregation-pipeline/

//https://www.mongodb.com/docs/manual/reference/operator/aggregation/match/#mongodb-pipeline-pipe.-match

//AGGREGATION_PIPELINE_STAGE_TYPE : MATCH;

MATCH : '$match';


//
//fragment CHARACTER: ( ESC | ~ ["\\]);
//STRING: '\'' CHARACTER* '\'';


// TODO: Is this the correct way to do this? GQL G4 does it differently, search for 'STRING:'
STRING: '\'' WORD '\'';

//BLOCK_STRING
//    :   '"""' .*? '"""'
//    ;

//fragment LOWERCASE : [a-z] ;
//fragment UPPERCASE : [A-Z] ;


//WORD : (LOWERCASE | UPPERCASE | '_')+ ;

WORD: [_A-Za-z] [_0-9A-Za-z]*;
COLON: ':';

WS: [ \t\n\r]+ -> skip;

//WHITESPACE : (' ' | '\t')+;
COMMA: ',';

//
//WHITESPACE : (' ' | '\t')+ ;
//
//NEWLINE : ('\r'? '\n' | '\r')+ ;
//
//TEXT : ('[' | '(') ~[\])]+ (']' | ')') ;
