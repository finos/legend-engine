parser grammar ElasticsearchConnectionParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = ElasticsearchConnectionLexerGrammar;
}

identifier:                             VALID_STRING | STRING | CLUSTER_DETAILS | AUTHENTICATION
;

v7ConnectionDefinition:                 (
                                            connectionStore
                                            | clusterDetails
                                            | authentication
                                        )*
                                        EOF
;

connectionStore:                        STORE COLON qualifiedName SEMI_COLON
;

clusterDetails:                         CLUSTER_DETAILS COLON islandDefinition SEMI_COLON
;

authentication:                         AUTHENTICATION COLON islandDefinition SEMI_COLON
;