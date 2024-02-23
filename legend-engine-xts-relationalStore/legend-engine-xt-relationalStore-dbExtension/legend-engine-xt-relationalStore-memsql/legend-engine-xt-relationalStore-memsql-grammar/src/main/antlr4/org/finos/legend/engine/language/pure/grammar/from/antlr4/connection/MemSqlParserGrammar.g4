parser grammar MemSqlParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = MemSqlLexerGrammar;
}

identifier:                                 VALID_STRING
;

memSqlDatasourceSpecification:                  MEMSQL
                                            BRACE_OPEN
                                                    (
                                                        host
                                                        | port
                                                        | databaseName
                                                        | useSsl
                                                    )*
                                            BRACE_CLOSE
;
host:                                  HOST COLON STRING SEMI_COLON
;
port:                                  PORT COLON STRING SEMI_COLON
;
databaseName:                          DATABASENAME COLON STRING SEMI_COLON
;
useSsl:                                USESSL COLON STRING SEMI_COLON
;
