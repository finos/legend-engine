// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.from.extension;

import org.finos.legend.engine.language.pure.grammar.from.connection.ConnectionValueSourceCode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;

import java.util.function.Function;

public interface ConnectionValueParser
{
    String getConnectionTypeName();

    Connection parse(ConnectionValueSourceCode sourceCode);

    static ConnectionValueParser newParser(String connectionTypeName, Function<ConnectionValueSourceCode, Connection> parser)
    {
        return new ConnectionValueParser()
        {
            @Override
            public String getConnectionTypeName()
            {
                return connectionTypeName;
            }

            @Override
            public Connection parse(ConnectionValueSourceCode sourceCode)
            {
                return parser.apply(sourceCode);
            }
        };
    }
}
