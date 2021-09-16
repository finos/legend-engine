parser grammar DataSpaceParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = DataSpaceLexerGrammar;
}

// -------------------------------------- IDENTIFIER --------------------------------------

identifier:                 VALID_STRING | STRING
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
dataSpaceElement:           DATA_SPACE qualifiedName
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
