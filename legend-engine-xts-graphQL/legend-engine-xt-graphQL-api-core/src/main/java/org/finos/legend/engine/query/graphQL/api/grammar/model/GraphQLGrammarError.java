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

package org.finos.legend.engine.query.graphQL.api.grammar.model;

import org.finos.legend.engine.language.graphQL.grammar.from.GraphQLParserException;

public class GraphQLGrammarError
{
    public boolean _error = true;
    public int line;
    public int column;
    public String message;

    public GraphQLGrammarError(GraphQLParserException e)
    {
        this.line = e.getSourceInformation().startLine;
        this.column = e.getSourceInformation().startColumn;
        this.message = e.getMessage();
    }
}
