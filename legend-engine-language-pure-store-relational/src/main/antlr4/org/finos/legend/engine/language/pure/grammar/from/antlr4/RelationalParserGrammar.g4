parser grammar RelationalParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = RelationalLexerGrammar;
}


// -------------------------------------- IDENTIFIER --------------------------------------

unquotedIdentifier:                         VALID_STRING
                                            | DATABASE | INCLUDE
                                            | SCHEMA | TABLE | VIEW | JOIN | FILTER | MULTIGRAIN_FILTER
                                            | AND | OR
                                            | MILESTONING | BUSINESS_MILESTONING | BUSINESS_MILESTONING_FROM | BUSINESS_MILESTONING_THRU
                                            | OUT_IS_INCLUSIVE | THRU_IS_INCLUSIVE | INFINITY_DATE | BUS_SNAPSHOT_DATE
                                            | PROCESSING_MILESTONING | PROCESSING_MILESTONING_IN | PROCESSING_MILESTONING_OUT
                                            | SCOPE | ENUMERATION_MAPPING | ASSOCIATION_MAPPING | OTHERWISE | INLINE
;

identifier:                                 unquotedIdentifier | STRING
;

// -------------------------------------- DEFINITION --------------------------------------

definition:                                 (database)*
                                            EOF
;
database:                                   DATABASE qualifiedName
                                                PAREN_OPEN
                                                    include*
                                                    (
                                                        schema
                                                        | table
                                                        | view
                                                        | join
                                                        | filter
                                                        | multiGrainFilter
                                                    )*
                                                PAREN_CLOSE
;
include:                                    INCLUDE qualifiedName
;


// -------------------------------------- SCHEMA & TABLE --------------------------------------

schema:                                     SCHEMA identifier
                                                PAREN_OPEN
                                                    (
                                                        table
                                                        | view
                                                    )*
                                                PAREN_CLOSE
;
table:                                      TABLE relationalIdentifier
                                                PAREN_OPEN
                                                    milestoneSpec?
                                                    (columnDefinition (COMMA columnDefinition)*)?
                                                PAREN_CLOSE
;
columnDefinition:                           relationalIdentifier identifier (PAREN_OPEN INTEGER (COMMA INTEGER)? PAREN_CLOSE)? (PRIMARY_KEY | NOT_NULL)?
;

// -------------------------------------- MILESTONING --------------------------------------

milestoneSpec:                              MILESTONING
                                                PAREN_OPEN
                                                    (milestoning (COMMA milestoning)*)?
                                                PAREN_CLOSE
;
milestoning:                                milestoningType
                                                PAREN_OPEN
                                                    milestoningSpecification
                                                PAREN_CLOSE
;
milestoningType:                            identifier
;
milestoningSpecification:                   (relationalIdentifier | COMMA | BOOLEAN | DATE | EQUAL)*
;
businessMilestoning:                        BUSINESS_MILESTONING
                                                PAREN_OPEN
                                                    (
                                                        businessMilestoningFrom
                                                        | bussinessSnapshotDate
                                                    )
                                                PAREN_CLOSE
;
businessMilestoningFrom:                    BUSINESS_MILESTONING_FROM EQUAL identifier COMMA
                                            BUSINESS_MILESTONING_THRU EQUAL identifier
                                            (COMMA THRU_IS_INCLUSIVE EQUAL BOOLEAN)?
                                            (COMMA INFINITY_DATE EQUAL DATE)?
;
bussinessSnapshotDate:                      BUS_SNAPSHOT_DATE EQUAL identifier
;
processingMilestoning:                      PROCESSING_MILESTONING
                                                PAREN_OPEN
                                                    PROCESSING_MILESTONING_IN EQUAL identifier COMMA
                                                    PROCESSING_MILESTONING_OUT EQUAL identifier
                                                    (COMMA OUT_IS_INCLUSIVE EQUAL BOOLEAN)?
                                                    (COMMA INFINITY_DATE EQUAL DATE)?
                                                PAREN_CLOSE
;


// -------------------------------------- VIEW --------------------------------------

view:                                       VIEW relationalIdentifier
                                                PAREN_OPEN
                                                    (viewFilterMapping)?
                                                    (viewGroupBy)?
                                                    (DISTINCT_CMD)?
                                                    (viewColumnMapping (COMMA viewColumnMapping)*)?
                                                PAREN_CLOSE
;
viewFilterMapping:                          FILTER_CMD (viewFilterMappingJoin | databasePointer)? identifier
;
viewFilterMappingJoin:                      databasePointer joinSequence PIPE databasePointer
;
viewGroupBy:                                GROUP_BY_CMD
                                                PAREN_OPEN
                                                    (operation (COMMA operation)*)?
                                                PAREN_CLOSE
;
viewColumnMapping:                          identifier (BRACKET_OPEN identifier BRACKET_CLOSE)? COLON operation PRIMARY_KEY?
;


// -------------------------------------- FILTER & JOIN --------------------------------------

filter:                                     FILTER identifier PAREN_OPEN operation PAREN_CLOSE
;
multiGrainFilter:                           MULTIGRAIN_FILTER identifier PAREN_OPEN operation PAREN_CLOSE
;
join:                                       JOIN identifier PAREN_OPEN operation PAREN_CLOSE
;

// -------------------------------------- OPERATIONS --------------------------------------

// NOTE: we have the `...Right` parser rule to avoid mutually left-recursive rules. For example, the rule
// `booleanOperation: operation booleanOperator operation` will cause ANTLR to throw error
// See https://github.com/antlr/antlr4/blob/master/doc/left-recursion.md
//
// Also note that we split the rule `operation` because in `joinOperation` we cannot directly use `joinOperation` as the operation
//
// IMPORTANT: Notice the way we construct `booleanOperation`, and `atomicOperation` which
// forms a hierarchy/precedence. The gist of this is:
//  - The more deeply nested the parser rule, the higher the precedence it is
//  - The higher precedence rule should use only token of precedence equals or higher than itself
//    (i.e. `atomicOperation` should not use `booleanOperation` in its parser definition)
// See https://stackoverflow.com/questions/1451728/antlr-operator-precedence

operation:                                  booleanOperation
                                            | joinOperation
;
booleanOperation:                           atomicOperation booleanOperationRight?
;
booleanOperationRight:                      booleanOperator operation
;
booleanOperator:                            AND | OR
;
atomicOperation:                            (
                                                groupOperation
                                                | ( databasePointer? functionOperation )
                                                | columnOperation
                                                | joinOperation
                                                | constant
                                            )
                                            atomicOperationRight?
;
atomicOperationRight:                       (atomicOperator atomicOperation) | atomicSelfOperator
;
atomicOperator:                             EQUAL | TEST_NOT_EQUAL | NOT_EQUAL | GREATER_THAN | LESS_THAN | GREATER_OR_EQUAL | LESS_OR_EQUAL
;
atomicSelfOperator:                         IS_NULL | IS_NOT_NULL
;
groupOperation:                             PAREN_OPEN operation PAREN_CLOSE
;
constant:                                   STRING | INTEGER | FLOAT
;
functionOperation:                          identifier PAREN_OPEN (functionOperationArgument (COMMA functionOperationArgument)*)? PAREN_CLOSE
;
functionOperationArgument:                  operation | functionOperationArgumentArray
;
functionOperationArgumentArray:             BRACKET_OPEN (functionOperationArgument (COMMA functionOperationArgument)*)? BRACKET_CLOSE
;
columnOperation:                            databasePointer? tableAliasColumnOperation
;
tableAliasColumnOperation:                  tableAliasColumnOperationWithTarget | tableAliasColumnOperationWithScopeInfo
;
tableAliasColumnOperationWithTarget:        TARGET DOT relationalIdentifier
;
tableAliasColumnOperationWithScopeInfo:     relationalIdentifier (DOT scopeInfo)?
;
joinOperation:                              databasePointer? joinSequence (PIPE (booleanOperation | tableAliasColumnOperation))?
;
joinSequence:                               joinPointer (GREATER_THAN joinPointerFull)*
;
joinPointer:                                AT identifier
;
joinPointerFull:                            (PAREN_OPEN identifier PAREN_CLOSE)? databasePointer? joinPointer
;


// -------------------------------------- RELATIONAL MAPPING --------------------------------------

// NOTE: Order must be preserved here as we want the associationMapping rule to be mapped as an associationMapping
// not as an embedded property mapping
mapping:                                    associationMapping | classMapping
;
associationMapping:                         ASSOCIATION_MAPPING
                                                PAREN_OPEN
                                                    propertyMapping (COMMA propertyMapping)*
                                                PAREN_CLOSE
                                                EOF
;
classMapping:                               mappingFilter?
                                            DISTINCT_CMD?
                                            mappingGroupBy?
                                            mappingPrimaryKey?
                                            mappingMainTable?
                                            (propertyMapping (COMMA propertyMapping)*)?
                                            EOF
;
mappingFilter:                              FILTER_CMD databasePointer (joinSequence PIPE databasePointer)? identifier
;
mappingGroupBy:                             GROUP_BY_CMD
                                                PAREN_OPEN
                                                    (operation (COMMA operation)*)?
                                                PAREN_CLOSE
;
mappingPrimaryKey:                          PRIMARY_KEY_CMD
                                                PAREN_OPEN
                                                    (operation (COMMA operation)*)?
                                                PAREN_CLOSE
;
mappingMainTable:                           MAIN_TABLE_CMD databasePointer mappingScopeInfo
;
mappingScopeInfo:                           relationalIdentifier (DOT scopeInfo)?
;


// -------------------------------------- PROPERTY MAPPING --------------------------------------

propertyMapping:                            singlePropertyMapping | propertyMappingWithScope
;
propertyMappingWithScope:                   SCOPE PAREN_OPEN databasePointer mappingScopeInfo? PAREN_CLOSE
                                                PAREN_OPEN
                                                    singlePropertyMapping (COMMA singlePropertyMapping)*
                                                PAREN_CLOSE
;
singlePropertyMapping:                      singlePropertyMappingWithPlus | singlePropertyMappingWithoutPlus
;
singlePropertyMappingWithPlus:              PLUS identifier localMappingProperty relationalPropertyMapping
;
singlePropertyMappingWithoutPlus:           identifier sourceAndTargetMappingId?
                                            (
                                                relationalPropertyMapping
                                                | embeddedPropertyMapping
                                                | inlineEmbeddedPropertyMapping
                                            )
;
sourceAndTargetMappingId:                   BRACKET_OPEN sourceId (COMMA targetId)? BRACKET_CLOSE
;
sourceId:                                   identifier
;
targetId:                                   identifier
;
relationalPropertyMapping:                  COLON (transformer)? operation
;
transformer:                                ENUMERATION_MAPPING identifier COLON
;


// -------------------------------------- LOCAL MAPPING PROPERTY --------------------------------------

localMappingProperty:                       COLON qualifiedName BRACKET_OPEN (localMappingPropertyFromMultiplicity DOT_DOT)? localMappingPropertyToMultiplicity BRACKET_CLOSE
;
localMappingPropertyFromMultiplicity:       INTEGER | STAR
;
localMappingPropertyToMultiplicity:         INTEGER | STAR
;


// -------------------------------------- EMBEDDED PROPERTY MAPPING --------------------------------------

embeddedPropertyMapping:                    PAREN_OPEN
                                            (
                                                mappingPrimaryKey?
                                                singlePropertyMapping (COMMA singlePropertyMapping)*
                                            )?
                                            PAREN_CLOSE (otherwiseEmbeddedPropertyMapping)?
;
inlineEmbeddedPropertyMapping:              PAREN_OPEN PAREN_CLOSE INLINE BRACKET_OPEN identifier BRACKET_CLOSE
;
otherwiseEmbeddedPropertyMapping:           OTHERWISE PAREN_OPEN otherwisePropertyMapping PAREN_CLOSE
;
otherwisePropertyMapping:                   BRACKET_OPEN identifier BRACKET_CLOSE COLON databasePointer? joinSequence
;


// -------------------------------------- BUILDING BLOCK --------------------------------------

scopeInfo:                                  relationalIdentifier (DOT relationalIdentifier)?
;
databasePointer:                            BRACKET_OPEN qualifiedName BRACKET_CLOSE
;
relationalIdentifier:                       unquotedIdentifier | QUOTED_STRING
;