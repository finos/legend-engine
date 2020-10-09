parser grammar AggregationAwareParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = AggregationAwareLexerGrammar;
}


// -------------------------------------- IDENTIFIER --------------------------------------

identifier:                                     VALID_STRING | STRING
                                                | ALL | LET | ALL_VERSIONS | ALL_VERSIONS_IN_RANGE      // from M3Parser
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
                                                    MAP_FN COLON combinedExpression COMMA AGGREGATE_FN COLON combinedExpression
                                                PAREN_CLOSE
;
