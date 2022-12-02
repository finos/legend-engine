parser grammar ${DbType}ParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = ${DbType}LexerGrammar;
}

identifier:                                 VALID_STRING
;

