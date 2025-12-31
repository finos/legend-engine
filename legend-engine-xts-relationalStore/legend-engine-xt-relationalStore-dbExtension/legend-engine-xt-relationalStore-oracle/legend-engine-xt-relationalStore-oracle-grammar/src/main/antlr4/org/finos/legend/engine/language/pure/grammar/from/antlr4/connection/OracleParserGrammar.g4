parser grammar OracleParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = OracleLexerGrammar;
}

identifier:                                 VALID_STRING
;

oracleDatasourceSpecification:         ORACLE
                                            BRACE_OPEN
                                                    (
                                                        host
                                                        | port
                                                        | serviceName
                                                    )*
                                            BRACE_CLOSE
;
host:                                  HOST COLON STRING SEMI_COLON
;
port:                                  PORT COLON STRING SEMI_COLON
;
serviceName:                           SERVICENAME COLON STRING SEMI_COLON
;

