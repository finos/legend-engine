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

package org.finos.legend.engine.persistence.iceberg;

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.engine.store.core.LegendStoreConnectionProvider;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/*
    A simple "catalog reader" that is used to demonstrate the use of the connection provider
 */
public class IcebergJdbcCatalogReader
{
    private LegendStoreConnectionProvider<Connection> connectionProvider;

    public IcebergJdbcCatalogReader(LegendStoreConnectionProvider<Connection> connectionProvider)
    {
        this.connectionProvider = connectionProvider;
    }

    public ImmutableSet<String> getNamespaces() throws Exception
    {
        MutableSet<String> namespaces = Sets.mutable.empty();
        try (Connection connection = connectionProvider.getConnection())
        {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("select * from iceberg_tables");
            while (resultSet.next())
            {
                String catalogName = resultSet.getString(1);
                String namespaceName = resultSet.getString(2);
                String tableName = resultSet.getString(3);
                String metadataLocation = resultSet.getString(3);
                namespaces.add(namespaceName);
            }
        }
        return namespaces.toImmutable();
    }
}
