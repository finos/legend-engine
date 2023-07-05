//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.plan.execution.stores.relational.result;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.LocalH2DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification;
import org.junit.Assert;
import org.junit.Test;

public class TestDatabaseIdentifiersCaseSensitiveVisitor
{
    @Test
    public void testDatabaseIdentifiersCaseSensitiveVisitorDefaultsToTrue()
    {
        RelationalDatabaseConnection databaseConnection = new RelationalDatabaseConnection();
        databaseConnection.datasourceSpecification = new LocalH2DatasourceSpecification();
        boolean result = databaseConnection.accept(new DatabaseIdentifiersCaseSensitiveVisitor());
        Assert.assertTrue(result);
    }

    @Test
    public void testDatabaseIdentifiersCaseSensitiveVisitorForSnowflakeDatasourceSpecification()
    {
        SnowflakeDatasourceSpecification snowflakeDatasourceSpecification = new SnowflakeDatasourceSpecification();
        RelationalDatabaseConnection databaseConnection = new RelationalDatabaseConnection();

        databaseConnection.datasourceSpecification = snowflakeDatasourceSpecification;
        boolean resultWithFlagNotSet = databaseConnection.accept(new DatabaseIdentifiersCaseSensitiveVisitor());
        Assert.assertTrue(resultWithFlagNotSet);

        snowflakeDatasourceSpecification.quotedIdentifiersIgnoreCase = false;
        boolean resultWithFlagSetAsFalse = databaseConnection.accept(new DatabaseIdentifiersCaseSensitiveVisitor());
        Assert.assertTrue(resultWithFlagSetAsFalse);

        snowflakeDatasourceSpecification.quotedIdentifiersIgnoreCase = true;
        boolean resultWithFlagSetAsTrue = databaseConnection.accept(new DatabaseIdentifiersCaseSensitiveVisitor());
        Assert.assertFalse(resultWithFlagSetAsTrue);
    }
}
