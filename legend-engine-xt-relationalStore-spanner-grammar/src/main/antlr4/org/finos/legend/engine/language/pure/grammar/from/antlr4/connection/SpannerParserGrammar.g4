parser grammar SpannerParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = SpannerLexerGrammar;
}

identifier:                                 VALID_STRING
;

spannerDatasourceSpecification:             SPANNER_DATA_SOURCE_SPECIFICATION
                                                BRACE_OPEN
                                                    (
                                                        dbHost
                                                        | dbPort
                                                        | projectId
                                                        | instanceId
                                                        | databaseId
                                                    )*
                                                BRACE_CLOSE
;

databaseId:                                 DATABASE COLON STRING SEMI_COLON
;
instanceId:                                 INSTANCE COLON STRING SEMI_COLON
;
projectId:                                    PROJECT COLON STRING SEMI_COLON
;
dbPort:                                     PORT COLON INTEGER SEMI_COLON
;
dbHost:                                     HOST COLON STRING SEMI_COLON
;