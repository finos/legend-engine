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
                                        | CONNECTIONSTORES
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
                                                (mappings | connections | connectionStoresList)*
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
connectionStoresList:                   CONNECTIONSTORES COLON
                                            BRACKET_OPEN
                                                (connectionStores (COMMA connectionStores)*)?
                                            BRACKET_CLOSE SEMI_COLON
;
storeConnections:                       qualifiedName COLON
                                            BRACKET_OPEN
                                                (identifiedConnection (COMMA identifiedConnection)*)?
                                            BRACKET_CLOSE
;
connectionStores:                       connection COLON
                                            BRACKET_OPEN
                                                (storeProviderPointer (COMMA storeProviderPointer)*)?
                                            BRACKET_CLOSE
;
storeProviderPointer:                   (storeProviderPointerType)? packageableElementPointer
;
storeProviderPointerType:               PAREN_OPEN VALID_STRING PAREN_CLOSE
;
identifiedConnection:                   identifier COLON (packageableElementPointer | embeddedConnection)
;
connection:                             packageableElementPointer
;
packageableElementPointer:              qualifiedName
;
embeddedConnection:                     ISLAND_OPEN (embeddedConnectionContent)*
;
embeddedConnectionContent:              ISLAND_START | ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_HASH | ISLAND_BRACE_CLOSE | ISLAND_END
;


// ---------------------------------- EMBEDDED RUNTIME ----------------------------------

embeddedRuntime:                        (mappings | connections)*
;
