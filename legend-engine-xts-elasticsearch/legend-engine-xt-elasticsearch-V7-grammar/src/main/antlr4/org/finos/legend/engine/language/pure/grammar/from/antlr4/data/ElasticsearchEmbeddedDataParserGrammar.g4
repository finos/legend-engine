// Copyright 2023 Goldman Sachs
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
parser grammar ElasticsearchEmbeddedDataParserGrammar;

import CoreParserGrammar;

options
{
    tokenVocab = ElasticsearchEmbeddedDataLexerGrammar;
}

identifier:                             VALID_STRING | STRING
;

indexName:                              identifier
;

indexJsonData:                          jsonObject | jsonArray
;

indexData:                              indexName COLON indexJsonData SEMI_COLON
;

definition:                            (indexData)*
                                       EOF
;

// TODO - move json parser to core to allow re-use
jsonObject:
                                        BRACE_OPEN jsonPair (COMMA jsonPair)* BRACE_CLOSE
                                      | BRACE_OPEN BRACE_CLOSE
;

jsonPair:                              JSON_STRING COLON jsonValue
;

jsonArray:                               BRACKET_OPEN jsonValue (COMMA jsonValue)* BRACKET_CLOSE
                                      | BRACKET_OPEN BRACKET_CLOSE
;

jsonValue:                             JSON_STRING
                                      | JSON_NUMBER
                                      | jsonObject
                                      | jsonArray
                                      | TRUE
                                      | FALSE
                                      | JSON_NULL
;