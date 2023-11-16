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
import org.finos.legend.connection.ConnectionExtension;
import org.finos.legend.connection.DatabaseType;

import java.util.List;
import java.util.Map;

public class RelationalConnectionExtension implements ConnectionExtension
{
    @Override
    public List<DatabaseType> getExtraDatabaseTypes()
    {
        return Lists.mutable.of(RelationalDatabaseType.values());
    }

    @Override
    public Map<String, DatabaseType> getExtraDatabaseTypeMapping()
    {
        return Maps.mutable.with(
                org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType.H2.name(), RelationalDatabaseType.H2,
                org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType.Snowflake.name(), RelationalDatabaseType.SNOWFLAKE,
                org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType.Postgres.name(), RelationalDatabaseType.POSTGRES,
                org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType.BigQuery.name(), RelationalDatabaseType.BIG_QUERY
        );
    }
}
