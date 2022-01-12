parser grammar FlatDataParserGrammar;

options
{
    tokenVocab = FlatDataLexerGrammar;
}


// -------------------------------------- IDENTIFIER -------------------------------------

identifier:                             VALID_STRING | STRING
                                        | SECTION | RECORD
                                        | RECORD_DATA_TYPE | OPTIONAL
                                        | FORMAT | TIME_ZONE | TRUE_STRING | FALSE_STRING
;


// -------------------------------------- DEFINITION -------------------------------------

definition:                             (section)*
                                        EOF
;
section:                                sectionSignature
                                            BRACE_OPEN
                                                (sectionProperty | sectionRecordType)*
                                            BRACE_CLOSE
;
sectionSignature:                       SECTION identifier COLON driverId
;
driverId:                               identifier
;
sectionProperty:                        booleanSectionProperty | nonBooleanSectionProperty
;
booleanSectionProperty:                 sectionPropertyName SEMI_COLON
;
nonBooleanSectionProperty:              sectionPropertyName COLON sectionPropertyValue SEMI_COLON
;
sectionPropertyName:                    identifier (DOT identifier)*
;
sectionPropertyValue:                   sectionPropertyValueLiteral | sectionPropertyValueArray
;
sectionPropertyValueLiteral:            INTEGER | STRING
;
sectionPropertyValueLiterals:           ( INTEGER (COMMA INTEGER)* ) | ( STRING (COMMA STRING)* )
;
sectionPropertyValueArray:              BRACKET_OPEN sectionPropertyValueLiterals? BRACKET_CLOSE
;
sectionRecordType:                      recordTypeSignature
                                            BRACE_OPEN
                                                recordTypeFields
                                            BRACE_CLOSE
;
recordTypeSignature:                    RECORD
;
recordTypeFields:                       recordTypeField*
;
recordTypeField:                        recordTypeLabel recordTypeAddress? COLON recordTypeDataType? SEMI_COLON
;
recordTypeLabel:                        identifier
;
recordTypeAddress:                      ADDRESS
;
recordTypeDataType:                     RECORD_DATA_TYPE recordTypeDataTypeAttributes?
;
recordTypeDataTypeAttributes:           PAREN_OPEN
                                            recordTypeDataTypeAttribute (COMMA recordTypeDataTypeAttribute)*
                                        PAREN_CLOSE
;
recordTypeDataTypeAttribute:            (recordTypeDataTypeAttributeName EQUAL recordTypeDataTypeAttributeValue) | OPTIONAL
;
recordTypeDataTypeAttributeName:        FORMAT | TIME_ZONE | TRUE_STRING | FALSE_STRING
;
recordTypeDataTypeAttributeValue:       STRING | BRACKET_OPEN STRING (COMMA STRING)* BRACKET_CLOSE
;
