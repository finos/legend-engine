parser grammar AthenaParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = AthenaLexerGrammar;
}

identifier:                                 VALID_STRING
;

athenaDsp:                                  ATHENA_DSP
                                                BRACE_OPEN
                                                    (
                                                        dbName
                                                        | awsRegion
                                                        | s3OutputLocation
                                                    )*
                                                BRACE_CLOSE
;
awsRegion:                                  AWS_REGION COLON STRING SEMI_COLON
;
s3OutputLocation:                           S3_OUTPUT_LOCATION COLON STRING SEMI_COLON
;
dbName:                                     NAME COLON STRING SEMI_COLON
;
