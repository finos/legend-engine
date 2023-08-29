parser grammar AcquisitionParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = AcquisitionLexerGrammar;
}

identifier:                      VALID_STRING | STRING | CONNECTION
;

// -------------------------------------- DEFINITION --------------------------------------

definition:                                 (
                                                fileAcquisition
                                                | legendServiceAcquisition
                                                | kafkaAcquisition
                                            )
                                            EOF
;

// --------------------------------------  FILE --------------------------------------
fileAcquisition:                        (
                                            filePath
                                            | fileType
                                            | headerLines
                                            | recordsKey
                                            | connection
                                            | fileSplittingKeys
                                        )*
                                        EOF
;
filePath:                               FILE_PATH COLON STRING SEMI_COLON
;
fileType:                               FILE_TYPE COLON fileTypeValue SEMI_COLON
;
headerLines:                            HEADER_LINES COLON INTEGER SEMI_COLON
;
recordsKey:                             RECORDS_KEY COLON STRING SEMI_COLON
;
fileSplittingKeys:                      FILE_SPLITTING_KEYS COLON
                                            BRACKET_OPEN
                                              (
                                                  STRING
                                                   (
                                                       COMMA
                                                       STRING
                                                   )*
                                               )
                                               BRACKET_CLOSE SEMI_COLON
;
fileTypeValue:                          (JSON_TYPE | XML_TYPE | CSV_TYPE)
;

// -------------------------------------- LEGEND SERVICE --------------------------------------
legendServiceAcquisition:                   (
                                                service
                                            )*
                                            EOF
;
service:                                  SERVICE COLON qualifiedName SEMI_COLON
;

// -------------------------------------- KAFKA --------------------------------------
kafkaAcquisition:                           (
                                                recordTag
                                                | dataType
                                                | connection
                                              )*
                                              EOF
;
recordTag:                                  RECORD_TAG COLON STRING SEMI_COLON
;
dataType:                                   DATA_TYPE COLON kafkaTypeValue SEMI_COLON
;
kafkaTypeValue:                             (JSON_TYPE | XML_TYPE)
;

// -------------------------------------- common ------------------------------------------------
connection:                         CONNECTION COLON qualifiedName SEMI_COLON
;