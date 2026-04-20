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


// -------------------- RELATION COMPARISON VALIDATION ------------------------------------
DATAQUALITYRELATIONCOMPARISON:           'DataQualityRelationComparison';
RECON_SOURCE:                            'source';
RECON_TARGET:                            'target';
RECON_KEYS:                              'keys';
COLUMNS_TO_COMPARE:                      'columnsToCompare';
RECON_STRATEGY:                          'strategy';
RECON_STRATEGY_MD5:                      'MD5Hash';
RECON_SOURCE_HASH_COLUMN:                'sourceHashColumn';
RECON_TARGET_HASH_COLUMN:                'targetHashColumn';
RECON_AGGREGATED_HASH:                   'aggregatedHash';
RECON_EXPECTED_MATCH:                    'expectedMatch';