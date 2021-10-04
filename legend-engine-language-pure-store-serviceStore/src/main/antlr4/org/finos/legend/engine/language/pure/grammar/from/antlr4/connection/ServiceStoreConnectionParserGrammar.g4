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

definition:                             (connectionStore | baseUrl)*
                                        EOF
;
connectionStore:                        STORE COLON qualifiedName SEMI_COLON
;
baseUrl:                                BASE_URL COLON identifier SEMI_COLON
;
