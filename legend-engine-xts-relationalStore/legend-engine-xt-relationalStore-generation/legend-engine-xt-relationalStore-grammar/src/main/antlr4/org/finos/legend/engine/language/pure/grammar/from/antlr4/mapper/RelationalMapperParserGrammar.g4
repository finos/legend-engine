parser grammar RelationalMapperParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = RelationalMapperLexerGrammar;
}

// -------------------------------------- IDENTIFIER --------------------------------------

unquotedIdentifier:                     VALID_STRING | IMPORT | INCLUDE | RELATIONAL_MAPPER
;
identifier:                             unquotedIdentifier | STRING
;


// -------------------------------------- DEFINITION --------------------------------------

imports:                                (importStatement)*
;
importStatement:                        IMPORT packagePath PATH_SEPARATOR STAR SEMI_COLON
;
definition:                             imports
                                            (relationalMapper)*
                                        EOF
;
relationalMapper:                       RELATIONALMAPPER qualifiedName
                                                PAREN_OPEN
                                                    (databaseMapperSection)?
                                                    (schemaMapperSection)?
                                                    (tableMapperSection)?
                                                PAREN_CLOSE
;
databaseMapperSection:                  DATABASEMAPPER COLON
                                        BRACKET_OPEN
                                            (databaseMapper(COMMA databaseMapper)*)?
                                        BRACKET_CLOSE
                                        SEMI_COLON
;
schemaMapperSection:                    SCHEMAMAPPER COLON
                                        BRACKET_OPEN
                                            (schemaMapper(COMMA schemaMapper)*)?
                                        BRACKET_CLOSE
                                        SEMI_COLON
;
tableMapperSection:                     TABLEMAPPER COLON
                                        BRACKET_OPEN
                                            (tableMapper(COMMA tableMapper)*)?
                                        BRACKET_CLOSE
                                        SEMI_COLON
;
include:                                INCLUDE qualifiedName
;


// -------------------------------------- DATABASE MAPPERS --------------------------------------

databaseMapper:                         BRACKET_OPEN
                                            (schemaReference (COMMA schemaReference)*)?
                                        BRACKET_CLOSE ARROW STRING

;
schemaReference:                        database DOT schema
;
database:                               qualifiedName
;
schema:                                 identifier
;

// -------------------------------------- SCHEMA MAPPERS --------------------------------------

schemaMapper:                           schemaReference ARROW STRING
;

// -------------------------------------- TABLE MAPPERS --------------------------------------

tableReference:                         database DOT schema DOT table
;
tableMapper:                            tableReference ARROW STRING
;
table:                                  identifier
;