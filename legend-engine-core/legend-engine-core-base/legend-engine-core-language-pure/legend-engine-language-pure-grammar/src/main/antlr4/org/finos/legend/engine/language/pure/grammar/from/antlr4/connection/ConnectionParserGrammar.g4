parser grammar ConnectionParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = ConnectionLexerGrammar;
}


// -------------------------------------- IDENTIFIER -------------------------------------

identifier:                             VALID_STRING | STRING
                                        | IMPORT
;


// -------------------------------------- DEFINITION -------------------------------------

definition:                             imports
                                            (connection)*
                                        EOF
;
imports:                                (importStatement)*
;
importStatement:                        IMPORT packagePath PATH_SEPARATOR STAR SEMI_COLON
;
connections:                            connection (COMMA connection)*
;
connection:                             connectionType qualifiedName
                                        connectionValue
;
connectionType:                         identifier
;
connectionValue:                        BRACE_OPEN (connectionValueContent)*
;
connectionValueContent:                 CONNECTION_ISLAND_BRACE_OPEN | CONNECTION_ISLAND_CONTENT | CONNECTION_ISLAND_BRACE_CLOSE
;
embeddedRuntimeConnection:              connectionType connectionValue
;