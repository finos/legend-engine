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

package org.finos.legend.engine.language.pure.grammar.integration.store.graphql.util;

import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.protocol.graphQLStore.metamodel.pure.GraphQLClassMapping;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class GraphQLMappingComposer
{
    public static String renderGraphQLClassMapping(GraphQLClassMapping graphQLClassMapping)
    {
        StringBuilder stringBuilder = new StringBuilder();

        if (graphQLClassMapping.typeName != null)
        {
            stringBuilder.append(getTabString(2)).append("~typeName [").append(graphQLClassMapping.storePath).append("] ").append(PureGrammarComposerUtility.convertString(graphQLClassMapping.typeName, true)).append("\n");
        }

        if (graphQLClassMapping.bindingPath != null)
        {
            stringBuilder.append(getTabString(2)).append("~binding ").append(graphQLClassMapping.bindingPath).append("\n");
        }

        if (graphQLClassMapping.joinAccessorQualifiedProperty != null)
        {
            stringBuilder.append(getTabString(2)).append("~joinAccessor ").append(PureGrammarComposerUtility.convertIdentifier(graphQLClassMapping.joinAccessorQualifiedProperty)).append("\n");
        }

        return stringBuilder.toString();
    }
}
