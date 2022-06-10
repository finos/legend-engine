parser grammar RelationalDatabaseConnectionParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = RelationalDatabaseConnectionLexerGrammar;
}


// -------------------------------------- IDENTIFIER -------------------------------------

identifier:                             VALID_STRING | STRING
                                        | STORE
                                        | TYPE | RELATIONAL_DATASOURCE_SPEC | RELATIONAL_AUTH_STRATEGY
                                        | DB_TIMEZONE | QUOTE_IDENTIFIERS
;


// -------------------------------------- DEFINITION -------------------------------------

definition:                             (
                                            connectionStore
                                            | dbType
                                            | dbConnectionTimezone
                                            | dbQuoteIdentifiers
                                            | relationalDBAuth
                                            | relationalDBDatasourceSpec
                                            | relationalPostProcessors
                                        )*
                                        EOF
;
connectionStore:                        STORE COLON qualifiedName SEMI_COLON
;
dbConnectionTimezone:                   DB_TIMEZONE COLON TIMEZONE SEMI_COLON
;
dbQuoteIdentifiers:                     QUOTE_IDENTIFIERS COLON BOOLEAN SEMI_COLON
;
dbType:                                 TYPE COLON identifier SEMI_COLON
;

relationalPostProcessors:               RELATIONAL_POST_PROCESSORS COLON
                                        BRACKET_OPEN
                                            (specification (COMMA specification)*)?
                                        BRACKET_CLOSE
                                        SEMI_COLON
;

relationalDBAuth:                       RELATIONAL_AUTH_STRATEGY COLON specification SEMI_COLON
;

relationalDBDatasourceSpec:             RELATIONAL_DATASOURCE_SPEC COLON specification SEMI_COLON
;


specification:                specificationType (specificationValueBody)?
;

specificationType:            VALID_STRING
;

specificationValueBody:       BRACE_OPEN (specificationValue)*
;

specificationValue:           SPECIFICATION_BRACE_OPEN | SPECIFICATION_CONTENT | SPECIFICATION_BRACE_CLOSE
;