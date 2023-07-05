parser grammar TextParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = TextLexerGrammar;
}

// -------------------------------------- IDENTIFIER --------------------------------------

identifier:                 VALID_STRING | STRING
                            | TEXT | TEXT_TYPE | TEXT_CONTENT
;


// -------------------------------------- DEFINITION --------------------------------------

definition:                 (textElement)*
                            EOF
;
textElement:                TEXT qualifiedName
                                BRACE_OPEN
                                    (textType | textContent)*
                                BRACE_CLOSE
;
textType:                   TEXT_TYPE COLON identifier SEMI_COLON
;
textContent:                TEXT_CONTENT COLON STRING SEMI_COLON
;
