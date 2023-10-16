parser grammar ConnectionParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = ConnectionLexerGrammar;
}

// -------------------------------------- IDENTIFIER --------------------------------------

identifier:                         VALID_STRING | STRING
                                    | ALL | LET | ALL_VERSIONS | ALL_VERSIONS_IN_RANGE | TO_BYTES_FUNCTION      // from M3Parser
                                    | CONNECTION_DEMO | RAW_VALUE
;

// -------------------------------------- DEFINITION --------------------------------------

definition:                         (connectionDemoElement)*
                                    EOF
;
connectionDemoElement:              CONNECTION_DEMO qualifiedName
                                        BRACE_OPEN
                                            (
                                                rawValue
                                            )*
                                        BRACE_CLOSE
;
rawValue:                           RAW_VALUE COLON ISLAND_OPEN (rawValueContent)* SEMI_COLON
;
rawValueContent:                    ISLAND_START | ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_HASH | ISLAND_BRACE_CLOSE | ISLAND_END
;