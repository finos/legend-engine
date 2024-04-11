parser grammar CoreParserGrammar;

qualifiedName:                                  (packagePath PATH_SEPARATOR)? identifier
;
packagePath:                                    identifier (PATH_SEPARATOR identifier)*
;

// Since BOOLEAN and INTEGER overlap with VALID_STRING, we have to account for them
// Also, here, we use `identifier` instead of VALID_STRING
// because in the main grammar, we will take care of keywords overlapping VALID_STRING
word:                                           identifier | BOOLEAN | INTEGER
;

islandDefinition:                           ISLAND_OPEN islandContent
;

islandContent:                              (ISLAND_START | ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_HASH | ISLAND_BRACE_CLOSE | ISLAND_END)*
;