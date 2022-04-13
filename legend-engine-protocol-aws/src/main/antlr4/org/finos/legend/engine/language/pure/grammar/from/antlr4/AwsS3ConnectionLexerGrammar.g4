lexer grammar AwsS3ConnectionLexerGrammar;

import CoreLexerGrammar;

// -------------------------------------- KEYWORD --------------------------------------

// COMMON
IMPORT:                                     'import';
NONE:                                       'None';

STORE:                                      'store';

//**********
// S3 CONNECTION
//**********

S3_PARTITION:                               'partition';
S3_REGION:                                  'region';
S3_BUCKET:                                  'bucket';

AWS:                                        'AWS';
AWS_CN:                                     'AWS_CN';
AWS_US_GOV:                                 'AWS_US_GOV';