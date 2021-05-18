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

import java.util.Collections;
import java.util.Map;

public class RelationalOperationElementGrammarToJsonInput
{
    public Map<String, String> operations = Collections.emptyMap();

    public RelationalOperationElementGrammarToJsonInput()
    {
        // DO NOT DELETE: this resets the default constructor for Jackson
    }

    public RelationalOperationElementGrammarToJsonInput(Map<String, String> operations)
    {
        this.operations = operations;
    }
}
