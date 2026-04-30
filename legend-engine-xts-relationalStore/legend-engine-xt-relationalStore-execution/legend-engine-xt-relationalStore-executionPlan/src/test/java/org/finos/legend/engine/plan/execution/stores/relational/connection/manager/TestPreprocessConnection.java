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

package org.finos.legend.engine.plan.execution.stores.relational.connection.manager;

import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TestPreprocessConnection
{
    // ─────────────────────────────────────────────────────────────────────────
    // Default implementation – returns the connection unchanged
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void testDefaultPreprocessConnectionReturnsOriginalConnection()
    {
        ConnectionManager defaultManager = new NoOpConnectionManager();
        RelationalDatabaseConnection original = buildConnection("127.0.0.1", 5432, "myDB");
        Identity identity = Identity.getAnonymousIdentity();
        Map<String, Result> allocationResults = new HashMap<>();
        allocationResults.put("allocVar", new ConstantResult("some-value"));

        DatabaseConnection result = defaultManager.preprocessConnection(original, identity, allocationResults);

        Assert.assertSame("Default preprocessConnection must return the exact same connection instance", original, result);
    }

    @Test
    public void testCustomPreprocessConnectionUsesAllocationResults()
    {
        ConnectionManager customManager = new AllocationAwareConnectionManager();
        RelationalDatabaseConnection original = buildConnection("127.0.0.1", 5432, "myDB");
        Identity identity = Identity.getAnonymousIdentity();

        Map<String, Result> allocationResults = new HashMap<>();
        allocationResults.put("dynamicHost", new ConstantResult("10.0.0.42"));
        allocationResults.put("dynamicPort", new ConstantResult(9999));
        allocationResults.put("dynamicDbName", new ConstantResult("enrichedDB"));

        DatabaseConnection result = customManager.preprocessConnection(original, identity, allocationResults);

        Assert.assertNotSame("Preprocessed connection should be a new instance", original, result);
        Assert.assertTrue(result instanceof RelationalDatabaseConnection);
        RelationalDatabaseConnection enriched = (RelationalDatabaseConnection) result;
        StaticDatasourceSpecification enrichedSpec = (StaticDatasourceSpecification) enriched.datasourceSpecification;

        Assert.assertEquals("10.0.0.42", enrichedSpec.host);
        Assert.assertEquals(9999, enrichedSpec.port);
        Assert.assertEquals("enrichedDB", enrichedSpec.databaseName);
    }

    @Test
    public void testCustomPreprocessConnectionReturnsOriginalWhenNoRelevantAllocations()
    {
        ConnectionManager customManager = new AllocationAwareConnectionManager();
        RelationalDatabaseConnection original = buildConnection("127.0.0.1", 5432, "myDB");
        Identity identity = Identity.getAnonymousIdentity();

        // No "dynamicHost" key – preprocessor should return unchanged
        Map<String, Result> allocationResults = new HashMap<>();
        allocationResults.put("unrelated", new ConstantResult("irrelevant"));

        DatabaseConnection result = customManager.preprocessConnection(original, identity, allocationResults);

        Assert.assertSame("When no relevant allocation vars are present, the original connection should be returned", original, result);
    }

    @Test
    public void testCustomPreprocessConnectionWithMultipleAllocationVars()
    {
        ConnectionManager customManager = new AllocationAwareConnectionManager();
        RelationalDatabaseConnection original = buildConnection("127.0.0.1", 5432, "myDB");
        Identity identity = Identity.getAnonymousIdentity();

        Map<String, Result> allocationResults = new HashMap<>();
        allocationResults.put("dynamicHost", new ConstantResult("prod-server.example.com"));
        // Only host is provided, port and dbName are not – should keep originals

        DatabaseConnection result = customManager.preprocessConnection(original, identity, allocationResults);

        Assert.assertNotSame(original, result);
        RelationalDatabaseConnection enriched = (RelationalDatabaseConnection) result;
        StaticDatasourceSpecification enrichedSpec = (StaticDatasourceSpecification) enriched.datasourceSpecification;

        Assert.assertEquals("prod-server.example.com", enrichedSpec.host);
        Assert.assertEquals("Port should remain unchanged", 5432, enrichedSpec.port);
        Assert.assertEquals("Database name should remain unchanged", "myDB", enrichedSpec.databaseName);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static RelationalDatabaseConnection buildConnection(String host, int port, String dbName)
    {
        StaticDatasourceSpecification spec = new StaticDatasourceSpecification();
        spec.host = host;
        spec.port = port;
        spec.databaseName = dbName;

        RelationalDatabaseConnection connection = new RelationalDatabaseConnection();
        connection.datasourceSpecification = spec;
        connection.authenticationStrategy = new TestDatabaseAuthenticationStrategy();
        connection.databaseType = DatabaseType.H2;
        connection.type = DatabaseType.H2;
        return connection;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stub ConnectionManagers for testing
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * A ConnectionManager that does NOT override preprocessConnection –
     * exercises the interface default.
     */
    private static class NoOpConnectionManager implements ConnectionManager
    {
        @Override
        public org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification getDataSourceSpecification(DatabaseConnection databaseConnection)
        {
            return null;
        }

        @Override
        public org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionKey generateKeyFromDatabaseConnection(DatabaseConnection databaseConnection)
        {
            return null;
        }

        @Override
        public java.sql.Connection getTestDatabaseConnection()
        {
            return null;
        }
    }

    /**
     * A ConnectionManager that overrides preprocessConnection to enrich the
     * connection based on allocation results from the execution state.
     * <p>
     * Looks for keys {@code dynamicHost}, {@code dynamicPort}, and {@code dynamicDbName}
     * in the allocation results and uses their values to build a new connection.
     */
    private static class AllocationAwareConnectionManager implements ConnectionManager
    {
        @Override
        public org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification getDataSourceSpecification(DatabaseConnection databaseConnection)
        {
            return null;
        }

        @Override
        public org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionKey generateKeyFromDatabaseConnection(DatabaseConnection databaseConnection)
        {
            return null;
        }

        @Override
        public java.sql.Connection getTestDatabaseConnection()
        {
            return null;
        }

        @Override
        public DatabaseConnection preprocessConnection(DatabaseConnection connection, Identity identity, Map<String, Result> allocationResults)
        {
            if (!(connection instanceof RelationalDatabaseConnection) || allocationResults == null)
            {
                return connection;
            }

            Result hostResult = allocationResults.get("dynamicHost");
            if (hostResult == null)
            {
                return connection;
            }

            RelationalDatabaseConnection original = (RelationalDatabaseConnection) connection;
            StaticDatasourceSpecification originalSpec = (StaticDatasourceSpecification) original.datasourceSpecification;

            // Build enriched connection from allocation results
            StaticDatasourceSpecification enrichedSpec = new StaticDatasourceSpecification();
            enrichedSpec.host = ((ConstantResult) hostResult).getValue().toString();

            Result portResult = allocationResults.get("dynamicPort");
            enrichedSpec.port = portResult != null ? ((Number) ((ConstantResult) portResult).getValue()).intValue() : originalSpec.port;

            Result dbResult = allocationResults.get("dynamicDbName");
            enrichedSpec.databaseName = dbResult != null ? ((ConstantResult) dbResult).getValue().toString() : originalSpec.databaseName;

            RelationalDatabaseConnection enriched = new RelationalDatabaseConnection();
            enriched.datasourceSpecification = enrichedSpec;
            enriched.authenticationStrategy = original.authenticationStrategy;
            enriched.databaseType = original.databaseType;
            enriched.type = original.type;
            return enriched;
        }
    }
}

