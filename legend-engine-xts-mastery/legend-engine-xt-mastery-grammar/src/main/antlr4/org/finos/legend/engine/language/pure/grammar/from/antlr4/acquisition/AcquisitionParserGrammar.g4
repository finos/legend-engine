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
                                            | maxRetryTimeInMinutes
                                            | encoding
                                            | decryption
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
maxRetryTimeInMinutes:                  MAX_RETRY_TIME_IN_MINUTES COLON INTEGER SEMI_COLON
;
fileTypeValue:                          (JSON_TYPE | XML_TYPE | CSV_TYPE)
;
encoding:                               ENCODING COLON STRING SEMI_COLON
;
decryption:                             DECRYPTION COLON (pgpDecryption | desDecryption)
                                        BRACE_CLOSE SEMI_COLON
;

// -------------------------------------- DECRYPTION --------------------------------------

pgpDecryption:                           PGP BRACE_OPEN
                                            ( privateKey | passPhrase)*
;

privateKey:                              PRIVATE_KEY COLON islandSpecification SEMI_COLON
;

passPhrase:                              PASS_PHRASE COLON islandSpecification SEMI_COLON
;

desDecryption:                           DES BRACE_OPEN
                                            ( decryptionKey | uuEncode | capOption)*
;

decryptionKey:                           DECRYPTION_KEY COLON islandSpecification SEMI_COLON
;

uuEncode:                                UU_ENCODE COLON boolean_value SEMI_COLON
;

capOption:                               CAP_OPTION COLON boolean_value SEMI_COLON
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
boolean_value:                      TRUE | FALSE
;

// -------------------------------------- ISLAND SPECIFICATION --------------------------------------
islandSpecification:                        islandType (islandValue)?
;
islandType:                                 identifier
;
islandValue:                                ISLAND_OPEN (islandValueContent)* ISLAND_END
;
islandValueContent:                         ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_BRACE_CLOSE | ISLAND_START | ISLAND_END
;