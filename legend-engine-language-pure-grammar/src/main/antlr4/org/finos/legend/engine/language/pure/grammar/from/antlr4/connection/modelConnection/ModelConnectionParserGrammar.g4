parser grammar ModelConnectionParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = ModelConnectionLexerGrammar;
}


// -------------------------------------- IDENTIFIER -------------------------------------

identifier:                             VALID_STRING | STRING
                                        | MODEL_CONNECTION_CLASS | URL
                                        | MODEL_CHAIN_CONNECTION_MAPPINGS
;


// -------------------------------------- DEFINITION -------------------------------------

definition:                             (modelConnectionClass | connectionUrl | modelChainMappings)*
                                        EOF
;
connectionUrl:                          URL COLON STRING SEMI_COLON
;
modelConnectionClass:                   MODEL_CONNECTION_CLASS COLON qualifiedName SEMI_COLON
;
mappings:                               BRACKET_OPEN (qualifiedName (COMMA qualifiedName)*)? BRACKET_CLOSE
;
modelChainMappings:                     MODEL_CHAIN_CONNECTION_MAPPINGS COLON mappings SEMI_COLON
;
