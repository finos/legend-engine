parser grammar AuthenticationStrategyParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = AuthenticationStrategyLexerGrammar;
}

identifier:                      VALID_STRING | STRING | NTLM_AUTHENTICATION | TOKEN_AUTHENTICATION
;

// -------------------------------------- DEFINITION --------------------------------------

definition:                                 (
                                                ntlmAuthentication
                                                | tokenAuthentication
                                            )
                                            EOF
;

// -------------------------------------- NTLMAuthentication --------------------------------------
ntlmAuthentication:             (
                                    credential
                                )*
                                EOF
;

// -------------------------------------- TokenAuthentication --------------------------------------
tokenAuthentication:            (
                                    tokenUrl | credential
                                )*
                                EOF
;
tokenUrl:                       TOKEN_URL COLON STRING SEMI_COLON
;

// -------------------------------------- Credential ------------------------------------------------
credential:                     CREDENTIAL COLON islandSpecification SEMI_COLON
;

// -------------------------------------- ISLAND SPECIFICATION --------------------------------------
islandSpecification:                        islandType (islandValue)?
;
islandType:                                 identifier
;
islandValue:                                ISLAND_OPEN (islandValueContent)* ISLAND_END
;
islandValueContent:                         ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_BRACE_CLOSE | ISLAND_START | ISLAND_END
;