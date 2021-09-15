parser grammar DataSpaceParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = DataSpaceLexerGrammar;
}

// -------------------------------------- IDENTIFIER --------------------------------------

identifier:                 VALID_STRING | STRING
                            | STEREOTYPES | TAGS
                            | DATA_SPACE
                            | DATA_SPACE_GROUP_ID
                            | DATA_SPACE_ARTIFACT_ID
                            | DATA_SPACE_VERSION_ID
                            | DATA_SPACE_MAPPING
                            | DATA_SPACE_RUNTIME
                            | DATA_SPACE_DESCRIPTION
                            | DATA_SPACE_DIAGRAMS
                            | DATA_SPACE_SUPPORT_EMAIL
;

// -------------------------------------- DEFINITION --------------------------------------

definition:                 (dataSpaceElement)*
                            EOF
;
dataSpaceElement:           DATA_SPACE stereotypes? taggedValues? qualifiedName
                                BRACE_OPEN
                                    (
                                        groupId
                                        | artifactId
                                        | versionId
                                        | mapping
                                        | runtime
                                        | description
                                        | diagrams
                                        | supportEmail
                                    )*
                                BRACE_CLOSE
;
stereotypes:                LESS_THAN LESS_THAN stereotype (COMMA stereotype)* GREATER_THAN GREATER_THAN
;
stereotype:                 qualifiedName DOT identifier
;
taggedValues:               BRACE_OPEN taggedValue (COMMA taggedValue)* BRACE_CLOSE
;
taggedValue:                qualifiedName DOT identifier EQUAL STRING
;
groupId:                    DATA_SPACE_GROUP_ID COLON STRING SEMI_COLON
;
artifactId:                 DATA_SPACE_ARTIFACT_ID COLON STRING SEMI_COLON
;
versionId:                  DATA_SPACE_VERSION_ID COLON STRING SEMI_COLON
;
mapping:                    DATA_SPACE_MAPPING COLON qualifiedName SEMI_COLON
;
runtime:                    DATA_SPACE_RUNTIME COLON qualifiedName SEMI_COLON
;
description:                DATA_SPACE_DESCRIPTION COLON STRING SEMI_COLON
;
diagrams:                   DATA_SPACE_DIAGRAMS COLON BRACKET_OPEN ( qualifiedName (COMMA qualifiedName)* )? BRACKET_CLOSE SEMI_COLON
;
supportEmail:               DATA_SPACE_SUPPORT_EMAIL COLON STRING SEMI_COLON
;
