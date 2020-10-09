parser grammar RuntimeParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = RuntimeLexerGrammar;
}


// -------------------------------------- IDENTIFIER --------------------------------------

identifier:                             VALID_STRING | STRING
                                        | RUNTIME | IMPORT
                                        | MAPPINGS | CONNECTIONS
;


// -------------------------------------- DEFINITION --------------------------------------

definition:                             imports
                                            (runtime)*
                                        EOF
;
imports:                                (importStatement)*
;
importStatement:                        IMPORT packagePath PATH_SEPARATOR STAR SEMI_COLON
;
runtime:                                RUNTIME qualifiedName
                                            BRACE_OPEN
                                                (mappings | connections)*
                                            BRACE_CLOSE
;
mappings:                               MAPPINGS COLON
                                            BRACKET_OPEN
                                                (qualifiedName (COMMA qualifiedName)*)?
                                            BRACKET_CLOSE SEMI_COLON
;
connections:                            CONNECTIONS COLON
                                            BRACKET_OPEN
                                                (storeConnections (COMMA storeConnections)*)?
                                            BRACKET_CLOSE SEMI_COLON
;
storeConnections:                       qualifiedName COLON
                                            BRACKET_OPEN
                                                (identifiedConnection (COMMA identifiedConnection)*)?
                                            BRACKET_CLOSE
;
identifiedConnection:                   identifier COLON (connectionPointer | embeddedConnection)
;
connectionPointer:                      qualifiedName
;
embeddedConnection:                     ISLAND_OPEN (embeddedConnectionContent)*
;
embeddedConnectionContent:              ISLAND_START | ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_HASH | ISLAND_BRACE_CLOSE | ISLAND_END
;


// ---------------------------------- EMBEDDED RUNTIME ----------------------------------

embeddedRuntime:                        (mappings | connections)*
;
