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

package org.finos.legend.engine.plan.execution.stores.relational.connection.postgres.test;

import org.finos.legend.engine.store.core.LegendStoreConnectionProvider;
import org.finos.legend.engine.store.core.LegendStoreSupport;

import java.sql.Connection;

public class LegendPostgresSupport implements LegendStoreSupport<Connection>
{
    @Override
    public LegendStoreConnectionProvider<Connection> getConnectionProvider()
    {
        // TODO - implement when this moved to a non-test module
        return null;
    }
}
