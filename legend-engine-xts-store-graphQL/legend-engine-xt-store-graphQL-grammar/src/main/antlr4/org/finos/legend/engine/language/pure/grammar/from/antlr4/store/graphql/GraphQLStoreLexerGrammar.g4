// Copyright 2026 Goldman Sachs
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

lexer grammar GraphQLStoreLexerGrammar;

import CoreLexerGrammar;

@lexer::members {
  static int lastTokenType = 0;
  public void emit(Token token) {
    super.emit(token);
    this.lastTokenType = token.getType();
  }
}

GRAPHQL_STORE:                              'GraphQLStore';
SCHEMA:                                     'schema';

// A '{' right after 'schema:' opens an unquoted embedded SDL block, captured as raw text and handed off
// to legend-engine-xt-graphQL-grammar's own parser - every other '{' (e.g. the GraphQLStore's own
// enclosing braces) stays an ordinary BRACE_OPEN. Mirrors MappingLexerGrammar's MAPPING_ISLAND_MODE.
BRACE_OPEN:                                 '{' {getVocabulary().getSymbolicName(this.lastTokenType).equals("COLON")}? {pushMode(GRAPHQL_SCHEMA_ISLAND_MODE);}
                                            | '{';

mode GRAPHQL_SCHEMA_ISLAND_MODE;
GRAPHQL_SCHEMA_ISLAND_BRACE_OPEN:           '{' -> pushMode(GRAPHQL_SCHEMA_ISLAND_MODE);
GRAPHQL_SCHEMA_ISLAND_BRACE_CLOSE:          '}' -> popMode;
GRAPHQL_SCHEMA_ISLAND_CONTENT:              (~[{}])+;
