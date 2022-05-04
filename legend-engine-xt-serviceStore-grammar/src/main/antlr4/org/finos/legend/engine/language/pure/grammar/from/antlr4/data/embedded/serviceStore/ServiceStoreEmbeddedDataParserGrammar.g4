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

parser grammar ServiceStoreEmbeddedDataParserGrammar;

import M3ParserGrammar;

options
{
    tokenVocab = ServiceStoreEmbeddedDataLexerGrammar;
}


// -------------------------------------- IDENTIFIER -------------------------------------

unquotedIdentifier:                         VALID_STRING
                                            | REQUEST_BLOCK_HEAD | RESPONSE_BLOCK_HEAD
                                            | REQUEST_METHOD | REQUEST_URL | REQUEST_URL_PATH | REQUEST_HEADER_PARAMETERS | REQUEST_QUERY_PARAMETERS | REQUEST_BODY_PATTERNS
                                            | RESPONSE_BODY
;
identifier:                                 unquotedIdentifier | STRING
;

// -------------------------------------- DEFINITION -------------------------------------

definition:                                 serviceStubMappings
                                            EOF
;
serviceStubMappings:                        BRACKET_OPEN (serviceStubMapping ( COMMA serviceStubMapping )*)? BRACKET_CLOSE
;
serviceStubMapping:                         BRACE_OPEN ( serviceRequestPattern | serviceResponseDefinition )* BRACE_CLOSE
;
serviceRequestPattern:                      REQUEST_BLOCK_HEAD COLON BRACE_OPEN
                                                (
                                                    serviceRequestMethodDefinition
                                                    | serviceRequestUrlPattern
                                                    | serviceRequestUrlPathPattern
                                                    | serviceRequestHeaderParametersPattern
                                                    | serviceRequestQueryParametersPattern
                                                    | serviceRequestBodyPatterns
                                                )*
                                            BRACE_CLOSE SEMI_COLON
;
serviceRequestMethodDefinition:             REQUEST_METHOD COLON identifier SEMI_COLON
;
serviceRequestUrlPattern:                   REQUEST_URL COLON STRING SEMI_COLON
;
serviceRequestUrlPathPattern:               REQUEST_URL_PATH COLON STRING SEMI_COLON
;
serviceRequestHeaderParametersPattern:      REQUEST_HEADER_PARAMETERS COLON BRACE_OPEN ( serviceRequestParameterPattern (COMMA serviceRequestParameterPattern)* ) BRACE_CLOSE SEMI_COLON
;
serviceRequestQueryParametersPattern:       REQUEST_QUERY_PARAMETERS COLON BRACE_OPEN ( serviceRequestParameterPattern (COMMA serviceRequestParameterPattern)* ) BRACE_CLOSE SEMI_COLON
;
serviceRequestParameterPattern:             serviceRequestParameterName COLON serviceRequestContentPattern
;
serviceRequestParameterName:                unquotedIdentifier | QUOTED_STRING
;
serviceRequestBodyPatterns:                 REQUEST_BODY_PATTERNS COLON BRACKET_OPEN ( serviceRequestContentPattern )* BRACKET_CLOSE SEMI_COLON
;
serviceRequestContentPattern:               identifier ISLAND_OPEN ( serviceRequestContentPatternContent )*
;
serviceRequestContentPatternContent:        ISLAND_START | ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_HASH | ISLAND_BRACE_CLOSE | ISLAND_END
;

serviceResponseDefinition:                  RESPONSE_BLOCK_HEAD COLON BRACE_OPEN (RESPONSE_BODY COLON embeddedData SEMI_COLON)* BRACE_CLOSE SEMI_COLON
;
embeddedData:                               identifier ISLAND_OPEN (embeddedDataContent)*
;
embeddedDataContent:                        ISLAND_START | ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_HASH | ISLAND_BRACE_CLOSE | ISLAND_END
;

