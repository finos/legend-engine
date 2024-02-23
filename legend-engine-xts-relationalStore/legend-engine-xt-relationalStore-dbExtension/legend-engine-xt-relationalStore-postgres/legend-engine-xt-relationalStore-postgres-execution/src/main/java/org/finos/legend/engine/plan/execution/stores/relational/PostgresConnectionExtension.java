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

package org.finos.legend.engine.plan.execution.stores.relational;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.postgres.PostgresCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.postgres.PostgresManager;

public class PostgresConnectionExtension implements RelationalConnectionExtension
{
    @Override
    public String type()
    {
        return "(Connection)ConnectionExtension";
    }

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "Postgres");
    }

    @Override
    public MutableList<DatabaseManager> getAdditionalDatabaseManager()
    {
        return Lists.mutable.of(new PostgresManager());
    }

    @Override
    public Boolean visit(StreamResultToTempTableVisitor visitor, RelationalDatabaseCommands databaseCommands)
    {
        if (databaseCommands instanceof PostgresCommands)
        {
            PostgresCommands postgresCommands = (PostgresCommands) databaseCommands;

            if (visitor.ingestionMethod == null)
            {
                visitor.ingestionMethod = postgresCommands.getDefaultIngestionMethod();
            }
            throw new UnsupportedOperationException("not yet implemented");
        }
        return null;
    }
}
