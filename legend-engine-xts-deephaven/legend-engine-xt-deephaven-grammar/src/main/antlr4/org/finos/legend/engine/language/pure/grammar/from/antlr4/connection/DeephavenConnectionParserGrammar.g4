parser grammar DeephavenConnectionParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = DeephavenConnectionLexerGrammar;
}

identifier:                             VALID_STRING | STRING | SERVER_URL | AUTHENTICATION
;

deephavenConnectionDefinition:         (
                                            connectionStore
                                            | serverUrlDefinition
                                            | authentication
                                        )*
                                        EOF
;


connectionStore:                        STORE COLON qualifiedName SEMI_COLON
;

serverUrlDefinition:                    SERVER_URL COLON serverUrl
;

serverUrl:                              STRING
;

authentication:                         AUTHENTICATION COLON islandDefinition SEMI_COLON
;