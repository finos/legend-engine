parser grammar CodeParserGrammar;

options
{
    tokenVocab = CodeLexerGrammar;
}

definition:             (section)*
                        EOF
;
section:                SECTION_START (sectionContent)*
;
sectionContent:         HASH | NON_HASH
;