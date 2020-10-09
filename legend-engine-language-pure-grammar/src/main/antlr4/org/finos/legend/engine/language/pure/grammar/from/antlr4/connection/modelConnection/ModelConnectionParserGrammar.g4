parser grammar ModelConnectionParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = ModelConnectionLexerGrammar;
}


// -------------------------------------- IDENTIFIER -------------------------------------

identifier:                             VALID_STRING | STRING
                                        | MODEL_CONNECTION_CLASS | URL
;


// -------------------------------------- DEFINITION -------------------------------------

definition:                             (modelConnectionClass | connectionUrl)*
                                        EOF
;
connectionUrl:                          URL COLON STRING SEMI_COLON
;
modelConnectionClass:                   MODEL_CONNECTION_CLASS COLON qualifiedName SEMI_COLON
;
