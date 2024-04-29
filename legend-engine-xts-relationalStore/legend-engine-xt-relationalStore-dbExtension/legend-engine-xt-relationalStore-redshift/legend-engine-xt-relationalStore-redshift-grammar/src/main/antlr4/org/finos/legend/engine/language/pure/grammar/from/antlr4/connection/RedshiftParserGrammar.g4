parser grammar RedshiftParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = RedshiftLexerGrammar;
}

identifier:                                 VALID_STRING
;

region:                                     REGION COLON STRING SEMI_COLON
;

endpointURL:                                 ENDPOINT_URL COLON STRING SEMI_COLON
;

clusterID:                                   CLUSTER_ID COLON STRING SEMI_COLON
;

redshiftDatasourceSpecification:               REDSHIFT
                                                BRACE_OPEN
                                                    (
                                                        dbHost
                                                        |region
                                                        |dbPort
                                                        |dbName
                                                        |endpointURL
                                                        |clusterID
                                                    )*
                                                BRACE_CLOSE
;
dbPort:                                     PORT COLON INTEGER SEMI_COLON
;
dbHost:                                     HOST COLON STRING SEMI_COLON
;
dbName:                                     NAME COLON STRING SEMI_COLON
;