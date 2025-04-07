parser grammar QueryGenerationConfigsParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = QueryGenerationConfigsLexerGrammar;
}

identifier:                      VALID_STRING | STRING |
                                 GENERATION_FEATURES_CONFIG | ENABLED | DISABLED
;

// -------------------------------------- Generation Features Config -------------------------------------
generationFeaturesConfig:        identifier
                                 BRACE_OPEN
                                 (
                                     enabledFeatures
                                     | disabledFeatures
                                 )*
                                 BRACE_CLOSE
                                 EOF
;

enabledFeatures:                 ENABLED COLON BRACKET_OPEN
                                    (STRING (COMMA STRING)*)?
                                 BRACKET_CLOSE SEMI_COLON
;

disabledFeatures:                DISABLED COLON BRACKET_OPEN
                                    (STRING (COMMA STRING)*)?
                                 BRACKET_CLOSE SEMI_COLON
;
