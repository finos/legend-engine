parser grammar AwsS3ConnectionParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = AwsS3ConnectionLexerGrammar;
}

// -------------------------------------- IDENTIFIER --------------------------------------

identifier:                                 VALID_STRING | STRING
                                            | STORE
                                            | IMPORT | NONE
                                            | S3_PARTITION | S3_REGION | S3_BUCKET
                                            | AWS | AWS_CN | AWS_US_GOV
;

// -------------------------------------- DEFINITION --------------------------------------

definition:                                 (
                                                connectionStore
                                                | partition
                                                | region
                                                | bucket
                                            )*
                                            EOF
;
connectionStore:                            STORE COLON qualifiedName SEMI_COLON
;
partition:                                  S3_PARTITION COLON
                                                (
                                                    AWS
                                                    | AWS_CN
                                                    | AWS_US_GOV
                                                )
                                            SEMI_COLON
;
region:                                     S3_REGION COLON STRING SEMI_COLON
;
bucket:                                     S3_BUCKET COLON STRING SEMI_COLON
;
