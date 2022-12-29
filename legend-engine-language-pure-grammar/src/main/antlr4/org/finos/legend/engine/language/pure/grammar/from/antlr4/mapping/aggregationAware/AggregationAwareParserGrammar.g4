parser grammar AggregationAwareParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = AggregationAwareLexerGrammar;
}


// -------------------------------------- IDENTIFIER --------------------------------------

identifier:                                     VALID_STRING | STRING
                                                | ALL | LET | ALL_VERSIONS | ALL_VERSIONS_IN_RANGE
                                                | BYTE_STREAM_FUNCTION      // from M3Parser
;

// -------------------------------------- DEFINITION --------------------------------------

aggregateSpecification:                         CAN_AGGREGATE BOOLEAN COMMA
                                                    GROUP_BY_FUNCTIONS
                                                        PAREN_OPEN
                                                            groupByFunctionSpecifications?
                                                        PAREN_CLOSE COMMA
                                                    AGGREGATE_VALUES
                                                        PAREN_OPEN
                                                            aggregationFunctionSpecifications?
                                                        PAREN_CLOSE
;
groupByFunctionSpecifications:                  groupByFunctionSpecification (COMMA groupByFunctionSpecification)*
;
groupByFunctionSpecification:                   combinedExpression
;
aggregationFunctionSpecifications:              aggregationFunctionSpecification (COMMA aggregationFunctionSpecification)*
;
aggregationFunctionSpecification:               PAREN_OPEN
                                                    MAP_FN COLON mapFunction COMMA AGGREGATE_FN COLON aggregateFunction
                                                PAREN_CLOSE
;
aggregateFunction:                              combinedExpression
;
mapFunction:                                    combinedExpression
;

// -------------------------------------- MAPPING --------------------------------------

aggregationAwareClassMapping :                  VIEWS COLON
                                                     BRACKET_OPEN
                                                         aggregationSpecification (COMMA aggregationSpecification)*
                                                     BRACKET_CLOSE
                                                     COMMA
                                                     mainMapping
                                                EOF
;

aggregationSpecification :                      PAREN_OPEN
                                                    modelOperation  COMMA   aggregateMapping
                                                PAREN_CLOSE
;

modelOperation :                                MODEL_OP COLON
                                                    BRACE_OPEN MAPPING_ISLAND_CONTENT MAPPING_ISLAND_BRACE_CLOSE
;

aggregateMapping :                              AGG_MAP COLON parserName
                                                    BRACE_OPEN MAPPING_ISLAND_CONTENT MAPPING_ISLAND_BRACE_CLOSE
;

mainMapping :                                   MAIN_MAP COLON parserName
                                                    BRACE_OPEN MAPPING_ISLAND_CONTENT MAPPING_ISLAND_BRACE_CLOSE
;

parserName :                                    VALID_STRING
;
