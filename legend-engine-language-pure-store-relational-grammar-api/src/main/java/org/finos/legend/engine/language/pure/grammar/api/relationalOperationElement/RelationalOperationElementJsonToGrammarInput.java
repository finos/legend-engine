// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.api.relationalOperationElement;

import org.finos.legend.engine.language.pure.grammar.from.ParserError;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.RelationalOperationElement;

import java.util.Map;

public class RelationalOperationElementJsonToGrammarInput
{
    public PureGrammarComposerContext.RenderStyle renderStyle = PureGrammarComposerContext.RenderStyle.STANDARD;
    public Map<String, RelationalOperationElement> operations;
    public Map<String, ParserError> operationErrors;

    public RelationalOperationElementJsonToGrammarInput()
    {
        // DO NOT DELETE: this resets the default constructor for Jackson
    }

    public RelationalOperationElementJsonToGrammarInput(Map<String, RelationalOperationElement> operations)
    {
        this.operations = operations;
    }
}
