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

package org.finos.legend.engine.language.pure.grammar.to;

import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.JsonModelConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.ModelChainConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.XmlModelConnection;

import java.util.Objects;
import java.util.Optional;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.unsupported;

public class HelperConnectionGrammarComposer
{
    // WIP: remove this method when we remove the connection visitor
    public static String getConnectionValueName(Connection connection, PureGrammarComposerContext context)
    {
        if (connection instanceof JsonModelConnection)
        {
            return "JsonModelConnection";
        }
        else if (connection instanceof XmlModelConnection)
        {
            return "XmlModelConnection";
        }
        else if (connection instanceof ModelChainConnection)
        {
            return "ModelChainConnection";
        }
        Optional<org.eclipse.collections.api.tuple.Pair<String, String>> connectionValueString = context.extraConnectionValueComposers.stream().map(composer -> composer.value(connection, context)).filter(Objects::nonNull).findFirst();
        return connectionValueString.orElseGet(() -> Tuples.pair(unsupported(connection.getClass(), "connection type"), null)).getOne();
    }
}
