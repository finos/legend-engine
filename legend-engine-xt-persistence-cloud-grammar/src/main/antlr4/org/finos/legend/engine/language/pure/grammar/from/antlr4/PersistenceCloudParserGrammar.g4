parser grammar PersistenceCloudParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = PersistenceCloudLexerGrammar;
}

// -------------------------------------- IDENTIFIER --------------------------------------

identifier:                                 VALID_STRING | STRING
                                            | AWS_GLUE_DPU_COUNT
;

// -------------------------------------- PLATFORM --------------------------------------

definition:                                 (
                                                awsGlueDpuCount
                                            )*
                                            EOF
;
awsGlueDpuCount:                            AWS_GLUE_DPU_COUNT COLON INTEGER SEMI_COLON
;
