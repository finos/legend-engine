parser grammar AuthorizerParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = AuthorizerLexerGrammar;
}


// -------------------------------------- IDENTIFIER -------------------------------------

identifier:                             VALID_STRING | STRING
                                        | IMPORT
;


// -------------------------------------- DEFINITION -------------------------------------

definition:                             imports
                                            (authorizer)*
                                        EOF
;
imports:                                (importStatement)*
;
importStatement:                        IMPORT packagePath PATH_SEPARATOR STAR SEMI_COLON
;
authorizer:                             authorizerType qualifiedName
                                        authorizerValue
;
authorizerType:                         identifier
;
authorizerValue:                        BRACE_OPEN (authorizerValueContent)*
;
authorizerValueContent:                 AUTHORIZER_ISLAND_BRACE_OPEN | AUTHORIZER_ISLAND_CONTENT | AUTHORIZER_ISLAND_BRACE_CLOSE
;