// Copyright 2022 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

lexer grammar ServiceStoreEmbeddedDataLexerGrammar;

import M3LexerGrammar;

// -------------------------------------- KEYWORD --------------------------------------

REQUEST_BLOCK_HEAD:                             'request';
RESPONSE_BLOCK_HEAD:                            'response';

REQUEST_METHOD:                                 'method';
REQUEST_URL:                                    'url';
REQUEST_URL_PATH:                               'urlPath';
REQUEST_HEADER_PARAMETERS:                      'headerParameters';
REQUEST_QUERY_PARAMETERS:                       'queryParameters';
REQUEST_BODY_PATTERNS:                          'bodyPatterns';

RESPONSE_BODY:                                  'body';


// -------------------------------------- BUILDING_BLOCK --------------------------------------

QUOTED_STRING:                                   ('"' ( EscSeq | ~["\r\n] )*  '"');