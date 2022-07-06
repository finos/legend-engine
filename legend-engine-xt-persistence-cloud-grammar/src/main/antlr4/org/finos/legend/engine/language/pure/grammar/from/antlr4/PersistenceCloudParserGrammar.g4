parser grammar PersistenceCloudParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = PersistenceCloudLexerGrammar;
}

// -------------------------------------- IDENTIFIER --------------------------------------

identifier:                                 VALID_STRING | STRING
                                            | PLATFORM_TYPE_AWS_GLUE
                                            | AWS_GLUE_DPU_COUNT
;

// -------------------------------------- PLATFORM --------------------------------------

platformAwsGlue:                            PLATFORM_TYPE_AWS_GLUE
                                                BRACE_OPEN
                                                    (awsGlueDpuCount)*
                                                BRACE_CLOSE
                                            SEMI_COLON
;
awsGlueDpuCount:                            AWS_GLUE_DPU_COUNT COLON INTEGER
;
