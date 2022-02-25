parser grammar ServiceStoreParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = ServiceStoreLexerGrammar;
}


// -------------------------------------- IDENTIFIER --------------------------------------

unquotedIdentifier:                         VALID_STRING
                                            | ALL | LET | ALL_VERSIONS | ALL_VERSIONS_IN_RANGE      // from M3Parser
                                            | SERVICE_STORE
                                            | DESCRIPTION | SERVICE_GROUP | SERVICE
                                            | PATH | REQUEST_BODY | METHOD | PARAMETERS | RESPONSE | SECURITY_SCHEME
                                            | ALLOW_RESERVED | LOCATION | STYLE | EXPLODE | ENUM
                                            | SERVICE_MAPPING | PARAM_MAPPING | SERVICE_REFERENCE
;

identifier:                                 unquotedIdentifier | STRING
;

// -------------------------------------- DEFINITION --------------------------------------

definition:                                 (serviceStore)*
                                            EOF
;
serviceStore:                               SERVICE_STORE qualifiedName
                                                PAREN_OPEN
                                                    ( description )?
                                                    ( serviceStoreElement )*
                                                PAREN_CLOSE
;
description:                                DESCRIPTION COLON identifier SEMI_COLON
;
serviceStoreElement:                        (service | serviceGroup)
;

// -------------------------------------- SERVICE_GROUP & SERVICE --------------------------------------

serviceGroup:                               SERVICE_GROUP identifier
                                                PAREN_OPEN
                                                    (
                                                        pathDefinition
                                                        | serviceStoreElement
                                                    )*
                                                PAREN_CLOSE
;
service:                                    SERVICE identifier
                                                PAREN_OPEN
                                                    (
                                                        pathDefinition
                                                        | bodyDefinition
                                                        | methodDefinition
                                                        | parametersDefinition
                                                        | responseDefinition
                                                        | securitySchemeDefinition
                                                    )*
                                                PAREN_CLOSE
;

// -------------------------------------- SERVICE_BUILDING_BLOCKS --------------------------------------

pathDefinition:                             PATH COLON identifier SEMI_COLON
;
bodyDefinition:                             REQUEST_BODY COLON typeReferenceDefinition SEMI_COLON
;
methodDefinition:                           METHOD COLON identifier SEMI_COLON
;
parametersDefinition:                       PARAMETERS COLON
                                                PAREN_OPEN
                                                    ( parameterDefinition (COMMA parameterDefinition)* )?
                                                PAREN_CLOSE
                                            SEMI_COLON
;
parameterDefinition:                        parameterName COLON typeReferenceDefinition ( PAREN_OPEN (parameterOptions (COMMA parameterOptions)*)  PAREN_CLOSE )?
;
parameterName:                              unquotedIdentifier | QUOTED_STRING
;
parameterOptions:                           allowReservedDefinition | locationDefinition | styleDefinition | explodeDefinition | enumDefinition
;
allowReservedDefinition:                    ALLOW_RESERVED EQUAL BOOLEAN
;
locationDefinition:                         LOCATION EQUAL identifier
;
styleDefinition:                            STYLE EQUAL identifier
;
explodeDefinition:                          EXPLODE EQUAL BOOLEAN
;
enumDefinition:                             ENUM EQUAL qualifiedName
;
responseDefinition:                         RESPONSE COLON typeReferenceDefinition SEMI_COLON
;
securitySchemeDefinition:                   SECURITY_SCHEME COLON BRACKET_OPEN (identifier (COMMA identifier)*)? BRACKET_CLOSE SEMI_COLON
;
typeReferenceDefinition:                    type | listType
;
type:                                       ( primitiveType | complexType)
;
primitiveType:                              identifier
;
complexType:                                qualifiedName INVERTED_ARROW qualifiedName
;
listType:                                   ( BRACKET_OPEN type BRACKET_CLOSE )
;

// -------------------------------------- SERVICE_STORE MAPPING --------------------------------------

mapping:                                    classMapping
;
classMapping:                               (localPropertyDefinition)*
                                            (serviceMapping)*
                                            EOF
;
localPropertyDefinition:                    PLUS identifier COLON type multiplicity SEMI_COLON
;
serviceMapping:                             SERVICE_MAPPING mappingService
                                            (PAREN_OPEN (pathMappingBlock)? (parametersMappingBlock)? (mappingBlock)? PAREN_CLOSE)?
;
mappingService:                             BRACKET_OPEN qualifiedName BRACKET_CLOSE servicePath
;
servicePath:                                identifier (DOT identifier)*
;
pathMappingBlock:                           PATH_MAPPING SERVICE_REFERENCE DOT RESPONSE (DOT identifier)*
;
parametersMappingBlock:                     PARAM_MAPPING PAREN_OPEN (parameterMapping (COMMA parameterMapping)*) PAREN_CLOSE
;
parameterMapping:                           parameterName COLON combinedExpression
;
mappingBlock:                               (elementMapping (COMMA elementMapping)*)
;
elementMapping:                             identifier COLON SERVICE_REFERENCE DOT PARAMETERS DOT parameterName
;