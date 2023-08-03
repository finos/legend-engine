parser grammar DatabricksParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = DatabricksLexerGrammar;
}

identifier:                                 VALID_STRING
;

