parser grammar DatabricksParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = DatabricksLexerGrammar;
}

identifier:                                 VALID_STRING
;

databricksDatasourceSpecification:          DATABRICKS
                                                BRACE_OPEN
                                                    (
                                                        hostname
                                                        | port
                                                        | protocol
                                                        | httpPath
                                                    )*
                                                BRACE_CLOSE
;

hostname:                                   HOSTNAME COLON STRING SEMI_COLON
;

port:                                       PORT COLON STRING SEMI_COLON
;

protocol:                                   PROTOCOL COLON STRING SEMI_COLON
;

httpPath:                                   HTTP_PATH COLON STRING SEMI_COLON
;