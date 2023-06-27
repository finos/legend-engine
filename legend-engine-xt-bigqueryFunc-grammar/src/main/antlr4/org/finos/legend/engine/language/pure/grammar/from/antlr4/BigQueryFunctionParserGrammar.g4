parser grammar BigQueryFunctionParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = BigQueryFunctionLexerGrammar;
}

identifier:     VALID_STRING | STRING |
                BIGQUERY_FUNCTION |
                BIGQUERY_FUNCTION__FUNCTION_NAME |
                BIGQUERY_FUNCTION__DESCRIPTION |
                BIGQUERY_FUNCTION__FUNCTION |
                BIGQUERY_FUNCTION__OWNER
                ;
// -------------------------------------- DEFINITION --------------------------------------

definition:                         (bigQueryFunction)*
                                    EOF
;
bigQueryFunction:               BIGQUERY_FUNCTION qualifiedName
                                        BRACE_OPEN
                                            (
                                                functionName
                                                | description
                                                | function
                                                | owner
                                            )*
                                        BRACE_CLOSE;

functionName:                   BIGQUERY_FUNCTION__FUNCTION_NAME COLON STRING SEMI_COLON;

description:                    BIGQUERY_FUNCTION__DESCRIPTION COLON STRING SEMI_COLON;

function:                       BIGQUERY_FUNCTION__FUNCTION COLON functionIdentifier SEMI_COLON;

owner :                         BIGQUERY_FUNCTION__OWNER COLON STRING SEMI_COLON;
