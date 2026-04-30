parser grammar AthenaParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = AthenaLexerGrammar;
}

identifier:                                 VALID_STRING
;

athenaDsp:                                  ATHENA_DSP
                                                BRACE_OPEN
                                                    (
                                                        athenaRegion
                                                        | athenaOutputLocation
                                                        | athenaDatabase
                                                        | athenaCatalog
                                                        | athenaWorkGroup
                                                        | athenaEndpoint
                                                    )*
                                                BRACE_CLOSE
;
athenaRegion:                                   ATHENA_REGION COLON STRING SEMI_COLON
;
athenaOutputLocation:                           ATHENA_OUTPUT_LOCATION COLON STRING SEMI_COLON
;
athenaDatabase:                                 ATHENA_DATABASE COLON STRING SEMI_COLON
;
athenaCatalog:                                  ATHENA_CATALOG COLON STRING SEMI_COLON
;
athenaWorkGroup:                                ATHENA_WORK_GROUP COLON STRING SEMI_COLON
;
athenaEndpoint:                                ATHENA_ENDPOINT COLON STRING SEMI_COLON
;
