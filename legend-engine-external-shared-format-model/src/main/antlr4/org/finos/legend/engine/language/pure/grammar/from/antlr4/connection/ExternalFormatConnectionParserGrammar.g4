parser grammar ExternalFormatConnectionParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = ExternalFormatConnectionLexerGrammar;
}


// -------------------------------------- IDENTIFIER -------------------------------------

identifier:                             VALID_STRING | STRING | STORE | EXTERNAL_SOURCE
;


// -------------------------------------- DEFINITION -------------------------------------

definition:                             (connectionStore | externalSource)*
                                        EOF
;
connectionStore:                        STORE COLON qualifiedName SEMI_COLON
;
externalSource:                         EXTERNAL_SOURCE COLON specification SEMI_COLON
;


specification:                          specificationType (specificationValueBody)?
;

specificationType:                      VALID_STRING
;

specificationValueBody:                 BRACE_OPEN (specificationValue)*
;

specificationValue:                     SPECIFICATION_BRACE_OPEN | SPECIFICATION_CONTENT | SPECIFICATION_BRACE_CLOSE
;