parser grammar ServiceStoreConnectionParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = ServiceStoreConnectionLexerGrammar;
}


// -------------------------------------- IDENTIFIER -------------------------------------

identifier:                             VALID_STRING | STRING | STORE | BASE_URL
;


// -------------------------------------- DEFINITION -------------------------------------

definition:                             (connectionStore | authenticationSpec | baseUrl)*
                                        EOF
;
connectionStore:                        STORE COLON qualifiedName SEMI_COLON
;

authenticationSpec:                     AUTH_SPECS COLON
                                        BRACE_OPEN
                                                ( authSpecificationObject ( COMMA authSpecificationObject )* )?
                                        BRACE_CLOSE
                                        SEMI_COLON
;

authSpecificationObject:                identifier COLON islandDefinition
;

baseUrl:                                BASE_URL COLON identifier SEMI_COLON
;
