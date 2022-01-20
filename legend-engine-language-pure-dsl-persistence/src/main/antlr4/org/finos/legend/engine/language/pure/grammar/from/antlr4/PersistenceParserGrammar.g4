parser grammar PersistenceParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = PersistenceLexerGrammar;
}

// ---------------------------------- IDENTIFIER -------------------------------------

identifier:                    SERVICE_PERSISTENCE
;