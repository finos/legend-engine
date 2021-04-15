parser grammar AuthenticationStrategyParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = AuthenticationStrategyLexerGrammar;
}

identifier:                      VALID_STRING
;

// ----------------------------- RELATIONAL DATABASE CONNECTION AUTH STRATEGY -----------------------------

defaultH2Auth:                          H2_DEFAULT_AUTH
;
testDBAuth:                             TEST_DB_AUTH
;
delegatedKerberosAuth:                  DELEGATED_KERBEROS_AUTH delegatedKerberosAuthConfig?
;
delegatedKerberosAuthConfig:            BRACE_OPEN
                                            (
                                                serverPrincipalConfig
                                            )*
                                        BRACE_CLOSE
;
serverPrincipalConfig:                  SERVER_PRINCIPAL COLON STRING SEMI_COLON
;