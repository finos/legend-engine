parser grammar BigQueryParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = BigQueryLexerGrammar;
}

identifier:                                 VALID_STRING
;

bigQueryDatasourceSpecification:            BIGQUERY_DSP
                                                BRACE_OPEN
                                                    (
                                                        projectId
                                                        | defaultDataset
                                                        | dbProxyHost
                                                        | dbProxyPort
                                                    )*
                                            BRACE_CLOSE
;
projectId:                                  PROJECT COLON STRING SEMI_COLON
;
defaultDataset:                             DATASET COLON STRING SEMI_COLON
;
dbProxyHost:                                PROXYHOST COLON STRING SEMI_COLON
;
dbProxyPort:                                PROXYPORT COLON STRING SEMI_COLON
;
