parser grammar DocumentDBParser;

import CoreParserGrammar;

options
{
    tokenVocab = DocumentDBLexer;
}

definition: DATABASE qualifiedName
            PAREN_OPEN
              include*
              (collection | collectionfragment)*
            PAREN_CLOSE
;

include: INCLUDE qualifiedName
;

collection:
    COLLECTION collectionIdentifier
    PAREN_OPEN
        fieldDefinitions
    PAREN_CLOSE
;

collectionfragment:
    DOCUMENTFRAGMENT collectionIdentifier
    PAREN_OPEN
        fieldDefinitions
    PAREN_CLOSE
;

fieldDefinitions: fieldDefinition (COMMA fieldDefinition)*
;

fieldDefinition:
    fieldIdentifier
    identifier (PRIMARYKEY | PARTIALKEY)?
;

collectionIdentifier: identifier
;

fieldIdentifier: identifier
;

identifier: VALID_STRING | STRING
;

qualifiedName: packagePath? identifier
;
