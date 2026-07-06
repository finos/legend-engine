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

package org.finos.legend.engine.plan.execution.stores.relational.plugin;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.plan.execution.nodes.ExecutionNodeExecutor;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.stores.relational.connection.AlloyTestServer;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.plan.execution.stores.relational.result.DatabaseIdentifiersCaseSensitiveVisitor;
import org.finos.legend.engine.plan.execution.stores.relational.result.SQLExecutionResult;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.SQLExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * End-to-end coverage that the preprocessed
 * {@link org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection}
 * flows through the whole plan-execution timeline against a live H2 database, not just the
 * moment of JDBC acquisition.
 * <p>
 * A {@link SpyPreprocessor} is inserted at the head of the {@link ConnectionManagerSelector}'s
 * manager list via reflection (the field is private with no setter — see the trade-offs
 * section of {@code copilot/preprocessed-connection-flow-test-plan.md}).
 * <p>
 * <b>JDBC-neutral observable:</b> these tests use {@link SpyPreprocessor#stampTimeZone(String)}
 * as the enrichment marker.  The {@code timeZone} field is read by result-processing (via
 * {@link org.finos.legend.engine.plan.execution.stores.relational.RelationalExecutor} which
 * threads it into {@link SQLExecutionResult#getDatabaseTimeZone()}) but does <b>not</b>
 * influence JDBC driver selection or the connection URL.  This keeps H2 as the actual DB
 * driver while giving us a strong observable at every downstream read point.
 * <p>
 * Flipping other fields (notably {@code type = Redshift}, which is the ideal case-sensitivity
 * knob) breaks JDBC acquisition in this test module because the executor's transformer
 * registry only wires H2.  See
 * {@code TestPreprocessedConnectionFlowAtSeam#testCaseSensitivity_seesFlippedTypeOnEnrichedConnection}
 * for the surgical (no-JDBC) case-sensitivity proof.
 */
public class TestPreprocessedConnectionFlowE2E extends AlloyTestServer
{
    // The AlloyTestServer test-data-insertion path connects to database "testDB" — see
    // RelationalConnectionManager.TEST_DB.  All plans below must therefore point at "testDB"
    // so JDBC connects to the DB that actually has the seeded rows.
    private static final String TEST_DB = "testDB";

    // Distinguishable timezone marker the spy stamps onto the enriched connection.
    private static final String ENRICHED_TZ = "US/Pacific";

    @Override
    protected void insertTestData(Statement statement) throws SQLException
    {
        statement.execute("drop table if exists preprocFlowTable");
        statement.execute("create table preprocFlowTable(id INT, name VARCHAR(200))");
        statement.execute("insert into preprocFlowTable (id, name) values (1, 'Alice')");
        statement.execute("insert into preprocFlowTable (id, name) values (2, 'Bob')");
    }

    @Test
    public void testSQLExecutionResult_retainsEnrichedNodeReference() throws Exception
    {
        SpyPreprocessor spy = SpyPreprocessor.stampTimeZone(ENRICHED_TZ);
        SingleExecutionPlan plan = buildSimpleSelectPlan(TEST_DB);
        SQLExecutionNode rawRoot = (SQLExecutionNode) plan.rootExecutionNode;
        Assert.assertNull("Sanity: raw plan node's connection.timeZone must start as null",
                rawRoot.connection.timeZone);

        SQLExecutionResult result = (SQLExecutionResult) executeWithSpy(plan, spy);

        try
        {
            Assert.assertEquals("Spy should have been invoked exactly once at the seam",
                    1, spy.preprocessCount.get());

            Assert.assertNotSame("SQLExecutionResult MUST hold the CLONED (enriched) node, not the raw plan node",
                    rawRoot, result.getSQLExecutionNode());

            RelationalDatabaseConnection retained = (RelationalDatabaseConnection) result.getSQLExecutionNode().connection;
            Assert.assertSame("The retained node's connection MUST be the EXACT instance the spy produced — " +
                            "this is the invariant every downstream .getSQLExecutionNode().connection read (13+ sites) relies on",
                    spy.producedOutputs.get(0), retained);
            Assert.assertEquals("Retained connection.timeZone MUST reflect the preprocessor's rewrite",
                    ENRICHED_TZ, retained.timeZone);

            Assert.assertNull("Raw plan node's connection.timeZone MUST remain null — proves no-mutation-of-input contract",
                    rawRoot.connection.timeZone);
        }
        finally
        {
            result.close();
        }
    }

    @Test
    public void testCaseSensitivity_endToEnd_readerSeesPreprocessedConnection() throws Exception
    {
        SpyPreprocessor spy = SpyPreprocessor.stampTimeZone(ENRICHED_TZ);
        SingleExecutionPlan plan = buildSimpleSelectPlan(TEST_DB);

        SQLExecutionResult result = (SQLExecutionResult) executeWithSpy(plan, spy);

        try
        {
            // SQLExecutionResult.databaseTimeZone is read from node.connection.timeZone in
            // RelationalExecutor.execute — same read pattern as buildTransformersAndBuilder's
            // case-sensitivity check (both go through the visited node's connection).
            Assert.assertEquals(
                    "SQLExecutionResult.getDatabaseTimeZone() MUST be the enriched value — proves that " +
                            "RelationalExecutor read from the enriched connection at result-construction time, " +
                            "which is the same read pattern used by RelationalResult.buildTransformersAndBuilder " +
                            "for the case-sensitivity decision (P3 / P7).",
                    ENRICHED_TZ, result.getDatabaseTimeZone());

            // Object identity: the retained connection IS the enriched instance.  This is what
            // the case-sensitivity visitor at RelationalResult:240 (and every graph-fetch
            // worker at RelationalExecutionNodeExecutor:2200 etc.) will read.
            RelationalDatabaseConnection retained = (RelationalDatabaseConnection) result.getSQLExecutionNode().connection;
            Assert.assertSame("Retained connection reference MUST equal the spy's output — " +
                            "the case-sensitivity visitor and all downstream 13+ readers observe THIS instance",
                    spy.producedOutputs.get(0), retained);
        }
        finally
        {
            result.close();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Plumbing
    // ─────────────────────────────────────────────────────────────────────────

    private static SingleExecutionPlan buildSimpleSelectPlan(String dbName) throws Exception
    {
        String planJson = "{\n" +
                "  \"serializer\": {\"name\": \"pure\", \"version\": \"vX_X_X\"},\n" +
                "  \"templateFunctions\": [],\n" +
                "  \"rootExecutionNode\": {\n" +
                "    \"_type\": \"sql\",\n" +
                "    \"resultType\": {\"_type\": \"dataType\", \"dataType\": \"meta::pure::metamodel::type::Any\"},\n" +
                "    \"executionNodes\": [],\n" +
                "    \"sqlQuery\": \"select id, name from preprocFlowTable order by id\",\n" +
                "    \"resultColumns\": [\n" +
                "      {\"label\": \"\\\"id\\\"\",   \"dataType\": \"INTEGER\"},\n" +
                "      {\"label\": \"\\\"name\\\"\", \"dataType\": \"VARCHAR(200)\"}\n" +
                "    ],\n" +
                "    \"connection\": {\n" +
                "      \"_type\": \"RelationalDatabaseConnection\",\n" +
                "      \"type\": \"H2\",\n" +
                "      \"databaseType\": \"H2\",\n" +
                "      \"authenticationStrategy\": {\"_type\": \"test\"},\n" +
                "      \"datasourceSpecification\": {\n" +
                "        \"_type\": \"static\",\n" +
                "        \"host\": \"127.0.0.1\",\n" +
                "        \"port\": " + serverPort + ",\n" +
                "        \"databaseName\": \"" + dbName + "\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        return objectMapper.readValue(planJson, SingleExecutionPlan.class);
    }

    private static Result executeWithSpy(SingleExecutionPlan plan, SpyPreprocessor spy) throws Exception
    {
        RelationalStoreExecutionState relState = new RelationalStoreExecutionState(new RelationalStoreState(serverPort));
        injectSpy(relState.getRelationalExecutor().getConnectionManager(), spy);

        ExecutionState state = new ExecutionState(
                Maps.mutable.empty(),
                Lists.mutable.withAll(plan.templateFunctions == null ? Lists.mutable.empty() : plan.templateFunctions),
                Lists.mutable.with(relState));

        return plan.rootExecutionNode.accept(new ExecutionNodeExecutor(Identity.getAnonymousIdentity(), state));
    }

    @SuppressWarnings("unchecked")
    private static void injectSpy(ConnectionManagerSelector selector, SpyPreprocessor spy) throws Exception
    {
        Field managers = ConnectionManagerSelector.class.getDeclaredField("connectionManagers");
        managers.setAccessible(true);
        ((MutableList<ConnectionManager>) managers.get(selector)).add(0, spy);
    }
}

