lexer grammar DataQualityLexerGrammar;

import M3LexerGrammar;

DATAQUALITYVALIDATION:                   'DataQualityValidation';
DQVALIDATIONCONSTRAINTS:                 'validationTree';
DQCONTEXT:                               'context';
FROM_DATASPACE:                          'fromDataSpace';
FROM_MAPPING_AND_RUNTIME:                'fromMappingAndRuntime';
FILTER:                                  'filter';
GRAPH_START:                             '$[';
GRAPH_END:                               ']$';



// ---------------------------------- BUILDING BLOCK --------------------------------------

SUBTYPE_START:                      '->subType(@';
