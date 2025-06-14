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


//----------------------- RELATION VALIDATION --------------------------------------------------
DATAQUALITYRELATIONVALIDATION:           'DataQualityRelationValidation';
RELATION_FUNCTION:                       'query';
VALIDATIONS:                             'validations';
VALIDATION_NAME:                         'name';
VALIDATION_DESCRIPTION:                  'description';
VALIDATION_ASSERTION:                    'assertion';
VALIDATION_TYPE:                         'type';
VALIDATION_TYPE_ROW:                     'ROW_LEVEL';
VALIDATION_TYPE_AGG:                     'AGGREGATE';