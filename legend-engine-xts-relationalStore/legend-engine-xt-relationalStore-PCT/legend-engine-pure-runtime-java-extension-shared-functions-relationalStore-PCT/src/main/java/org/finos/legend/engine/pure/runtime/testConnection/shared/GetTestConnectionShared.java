// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.pure.runtime.testConnection.shared;

import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.pure.m3.exception.PureExecutionException;

public class GetTestConnectionShared
{
    public static RelationalDatabaseConnection getDatabaseConnection(DatabaseType dbType)
    {
        TestConnectionIntegration found = TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == dbType).getFirst();
        if (found == null)
        {
            throw new PureExecutionException("Can't find a TestConnectionIntegration for dbType " + dbType + ". Available ones are " + TestConnectionIntegrationLoader.extensions().collect(c -> c.getDatabaseType().name()));
        }
        return found.getConnection();
    }

}
