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

package org.finos.legend.engine.language.sql.grammar.integration;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.language.sql.grammar.to.SQLGrammarComposer;
import org.finos.legend.engine.protocol.sql.metamodel.Node;

import java.util.Map;

public class SQLPureGrammarComposerExtension implements PureGrammarComposerExtension
{
    @Override
    public Map<String, Function2<Object, PureGrammarComposerContext, String>> getExtraEmbeddedPureComposers()
    {
        return Maps.mutable.with(
                "SQL",
                (element, context) -> SQLGrammarComposer.newInstance().renderNode((Node) element)
        );
    }
}
