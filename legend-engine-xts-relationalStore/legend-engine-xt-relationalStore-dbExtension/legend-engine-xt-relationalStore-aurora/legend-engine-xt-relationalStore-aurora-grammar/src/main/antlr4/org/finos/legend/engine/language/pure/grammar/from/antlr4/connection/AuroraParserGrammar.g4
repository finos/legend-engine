parser grammar AuroraParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = AuroraLexerGrammar;
}

identifier:                                 VALID_STRING
;

auroraDsp:                                  AURORA_DSP
                                                BRACE_OPEN
                                                    (
                                                        auroraHost
                                                        | auroraPort
                                                        | auroraName
                                                        | auroraClusterInstanceHostPattern
                                                    )*
                                                BRACE_CLOSE
;
auroraHost:                                     AURORA_HOST COLON STRING SEMI_COLON
;
auroraPort:                                     AURORA_PORT COLON INTEGER SEMI_COLON
;
auroraName:                                     AURORA_NAME COLON STRING SEMI_COLON
;
auroraClusterInstanceHostPattern:               AURORA_CLUSTER_INSTANCE_HOST_PATTERN COLON STRING SEMI_COLON
;

globalAuroraDsp:                            GLOBAL_AURORA_DSP
                                                BRACE_OPEN
                                                    (
                                                        globalAuroraHost
                                                        | globalAuroraPort
                                                        | globalAuroraName
                                                        | globalAuroraRegion
                                                        | globalAuroraClusterInstanceHostPatterns
                                                    )*
                                                BRACE_CLOSE
;
globalAuroraHost:                               AURORA_HOST COLON STRING SEMI_COLON
;
globalAuroraPort:                               AURORA_PORT COLON INTEGER SEMI_COLON
;
globalAuroraName:                               AURORA_NAME COLON STRING SEMI_COLON
;
globalAuroraRegion:                             AURORA_REGION COLON STRING SEMI_COLON
;
globalAuroraClusterInstanceHostPatterns:         AURORA_GLOBAL_CLUSTER_INSTANCE_HOST_PATTERNS COLON
                                                    BRACKET_OPEN
                                                        STRING (COMMA STRING)*
                                                    BRACKET_CLOSE SEMI_COLON
;
