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

package org.finos.legend.connection.impl;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.connection.Connection;
import org.finos.legend.connection.ConnectionProvider;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This is the instrumented version of {@link ConnectionProvider} which is used for testing.
 */
public class InstrumentedConnectionProvider implements ConnectionProvider
{
    private final Map<String, Connection> connectionsIndex = Maps.mutable.empty();

    public void injectConnection(Connection connection)
    {
        if (this.connectionsIndex.containsKey(connection.getIdentifier()))
        {
            throw new RuntimeException(String.format("Found multiple connections with identifier '%s'", connection.getIdentifier()));
        }
        this.connectionsIndex.put(connection.getIdentifier(), connection);
    }

    @Override
    public Connection lookup(String identifier)
    {
        return Objects.requireNonNull(this.connectionsIndex.get(identifier), String.format("Can't find connection with identifier '%s'", identifier));
    }

    @Override
    public List<Connection> getAll()
    {
        return Lists.mutable.withAll(connectionsIndex.values());
    }
}
