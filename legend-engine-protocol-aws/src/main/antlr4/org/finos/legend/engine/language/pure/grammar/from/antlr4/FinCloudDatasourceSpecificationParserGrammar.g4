parser grammar FinCloudDatasourceSpecificationParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = FinCloudDatasourceSpecificationLexerGrammar;
}

identifier:                      VALID_STRING
;

// ----------------------------- RELATIONAL DATABASE CONNECTION DATASOURCE SPEC -----------------------------

finCloudDatasourceSpec:                     FIN_CLOUD
                                                BRACE_OPEN
                                                    (
                                                        apiUrl
                                                    )*
                                                BRACE_CLOSE
;
apiUrl:                                     API_URL COLON STRING SEMI_COLON
;
