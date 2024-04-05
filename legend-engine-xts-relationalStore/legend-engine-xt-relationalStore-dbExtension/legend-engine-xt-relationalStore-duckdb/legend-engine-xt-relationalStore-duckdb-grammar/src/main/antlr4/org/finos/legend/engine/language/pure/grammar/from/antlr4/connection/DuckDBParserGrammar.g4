parser grammar DuckDBParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = DuckDBLexerGrammar;
}

identifier:                                 VALID_STRING
;

duckDBDatasourceSpecification:          DUCKDB
                                                BRACE_OPEN
                                                    (
                                                        path
                                                    )*
                                                BRACE_CLOSE
;

path:                                       PATH COLON STRING SEMI_COLON
;