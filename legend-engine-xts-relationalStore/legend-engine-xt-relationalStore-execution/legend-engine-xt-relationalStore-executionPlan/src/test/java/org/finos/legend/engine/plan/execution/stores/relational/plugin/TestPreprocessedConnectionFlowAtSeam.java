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
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.plan.execution.stores.relational.result.DatabaseIdentifiersCaseSensitiveVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.AllocationExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.CreateAndPopulateTempTableExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.RelationalExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.RelationalSaveNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.SQLExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.DataTypeResultType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.result.SQLResultColumn;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Surgical, no-JDBC tests that prove the preprocess seam at the top of
 * {@link RelationalExecutionNodeExecutor#visit(ExecutionNode)} produces an enriched node whose
 * connection is what all downstream readers observe.
 * <p>
 * The seam is invoked reflectively so its {@code private} visibility can be preserved.  A
 * {@link SpyPreprocessor} is injected at the head of the {@link ConnectionManagerSelector}'s
 * manager list (also via reflection — {@code connectionManagers} is a private mutable field
 * with no setter).
 */
public class TestPreprocessedConnectionFlowAtSeam
{
    private static final int TEST_PORT = 0; // no JDBC opened by these tests

    // ─────────────────────────────────────────────────────────────────────────
    // Enriched connection is on a distinct node instance
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void testEnrichedConnection_returnedFromSeam_isDistinctInstance() throws Exception
    {
        SpyPreprocessor spy = SpyPreprocessor.stampDatabaseName("PREPROCESSED");
        SeamHarness harness = SeamHarness.withSpy(spy);

        SQLExecutionNode raw = new SQLExecutionNode();
        raw.connection = buildConnection("raw");

        ExecutionNode enriched = harness.invokeSeam(raw);

        Assert.assertNotSame("Seam must return a cloned node when preprocessor enriches the connection", raw, enriched);
        Assert.assertTrue("Cloned node must be a SQLExecutionNode", enriched instanceof SQLExecutionNode);

        RelationalDatabaseConnection enrichedConn = (RelationalDatabaseConnection) ((SQLExecutionNode) enriched).connection;
        Assert.assertEquals("raw-PREPROCESSED",
                ((StaticDatasourceSpecification) enrichedConn.datasourceSpecification).databaseName);

        // No-mutation invariant on the input node
        Assert.assertEquals("Original node's connection must NOT be mutated",
                "raw",
                ((StaticDatasourceSpecification) ((RelationalDatabaseConnection) raw.connection).datasourceSpecification).databaseName);

        Assert.assertEquals("Preprocessor should be invoked once per seam entry", 1, spy.preprocessCount.get());
    }

    @Test
    public void testSQLExecutionNode_shallowCopyReplacesConnectionAndPreservesOtherFields()
    {
        SQLExecutionNode original = new SQLExecutionNode();
        // ExecutionNode base fields
        original.resultType = new DataTypeResultType();
        original.executionNodes = Arrays.asList(new AllocationExecutionNode(), new AllocationExecutionNode());
        original.authDependent = true;
        // SQLExecutionNode fields
        original.sqlComment = "-- test";
        original.sqlQuery = "select 1";
        original.onConnectionCloseCommitQuery = "commit-q";
        original.onConnectionCloseRollbackQuery = "rollback-q";
        original.connection = buildConnection("originalDb");
        original.resultColumns = Collections.singletonList(new SQLResultColumn("c", "VARCHAR(100)"));
        original.isResultColumnsDynamic = true;
        original.isMutationSQL = true;

        DatabaseConnection replacement = buildConnection("newDb");

        SQLExecutionNode copy = original.shallowCopyWithConnection(replacement);

        Assert.assertNotSame("copy must be a new instance", original, copy);
        Assert.assertSame("connection must be the supplied replacement", replacement, copy.connection);
        Assert.assertNotSame("connection must NOT be the original", original.connection, copy.connection);

        // Every other field is copied by reference.
        Assert.assertSame(original.resultType, copy.resultType);
        Assert.assertSame(original.executionNodes, copy.executionNodes);
        Assert.assertEquals(original.authDependent, copy.authDependent);
        Assert.assertSame(original.sqlComment, copy.sqlComment);
        Assert.assertSame(original.sqlQuery, copy.sqlQuery);
        Assert.assertSame(original.onConnectionCloseCommitQuery, copy.onConnectionCloseCommitQuery);
        Assert.assertSame(original.onConnectionCloseRollbackQuery, copy.onConnectionCloseRollbackQuery);
        Assert.assertSame(original.resultColumns, copy.resultColumns);
        Assert.assertEquals(original.isResultColumnsDynamic, copy.isResultColumnsDynamic);
        Assert.assertEquals(original.isMutationSQL, copy.isMutationSQL);

        // Original must NOT be mutated.
        Assert.assertNotSame(replacement, original.connection);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cross-cutting: verify that node types NOT touched by the helper cannot
    // hide surprising deep-copy semantics.  A sub-node reference held in
    // executionNodes must be the *same* reference in the copy.
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void testShallowCopyPreservesSubNodeIdentity()
    {
        AllocationExecutionNode child = new AllocationExecutionNode();
        SQLExecutionNode parent = new SQLExecutionNode();
        parent.executionNodes = Collections.singletonList(child);
        parent.connection = buildConnection("d");

        SQLExecutionNode copy = parent.shallowCopyWithConnection(buildConnection("d2"));

        Assert.assertSame("children must be reused by reference (shallow copy)",
                child, copy.executionNodes.get(0));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Case-sensitivity visitor sees the flipped type on the enriched node
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void testCaseSensitivity_seesFlippedTypeOnEnrichedConnection() throws Exception
    {
        // Raw node has type = H2 (non-Redshift → case-sensitive path → visitor returns TRUE)
        // Spy flips type to Redshift → visitor short-circuits and returns FALSE
        SpyPreprocessor spy = SpyPreprocessor.flipType(DatabaseType.Redshift);
        SeamHarness harness = SeamHarness.withSpy(spy);

        SQLExecutionNode raw = new SQLExecutionNode();
        raw.connection = buildConnection("someDb");
        Assert.assertEquals("Sanity: raw connection must start as H2", DatabaseType.H2, raw.connection.type);

        ExecutionNode enrichedNode = harness.invokeSeam(raw);
        RelationalDatabaseConnection enrichedConn = (RelationalDatabaseConnection) ((SQLExecutionNode) enrichedNode).connection;
        RelationalDatabaseConnection rawConn = (RelationalDatabaseConnection) raw.connection;

        Assert.assertEquals("Enriched connection must carry flipped type", DatabaseType.Redshift, enrichedConn.type);
        Assert.assertEquals("Raw connection type must remain untouched", DatabaseType.H2, rawConn.type);

        DatabaseIdentifiersCaseSensitiveVisitor visitor = new DatabaseIdentifiersCaseSensitiveVisitor();
        Assert.assertEquals("Enriched connection (type=Redshift) → visitor MUST report case-Insensitive (false)",
                Boolean.FALSE, enrichedConn.accept(visitor));
        Assert.assertEquals("Raw connection (type=H2)         → visitor MUST report case-sensitive (true)",
                Boolean.TRUE, rawConn.accept(visitor));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Non-connection-bearing node types bypass the preprocessor
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void testPreprocessor_notInvokedForNonConnectionBearingNodes() throws Exception
    {
        SpyPreprocessor spy = SpyPreprocessor.stampDatabaseName("SHOULD-NOT-APPEAR");
        SeamHarness harness = SeamHarness.withSpy(spy);

        AllocationExecutionNode alloc = new AllocationExecutionNode();
        ExecutionNode returned = harness.invokeSeam(alloc);

        Assert.assertSame("AllocationExecutionNode does not carry .connection — seam MUST pass it through by reference", alloc, returned);
        Assert.assertEquals("Preprocessor must NOT be invoked for non-.connection-bearing node types",
                0, spy.preprocessCount.get());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Seam handles all four .connection-bearing node types
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("deprecation")
    public void testAllFourNodeTypes_areEnrichedByTheSeam() throws Exception
    {
        SpyPreprocessor spy = SpyPreprocessor.stampDatabaseName("ENRICHED");
        SeamHarness harness = SeamHarness.withSpy(spy);

        SQLExecutionNode sqlNode = new SQLExecutionNode();
        sqlNode.connection = buildConnection("sql");

        RelationalExecutionNode relNode = new RelationalExecutionNode();
        relNode.connection = buildConnection("rel");

        RelationalSaveNode saveNode = new RelationalSaveNode();
        saveNode.connection = buildConnection("save");

        CreateAndPopulateTempTableExecutionNode tempNode = new CreateAndPopulateTempTableExecutionNode();
        tempNode.connection = buildConnection("temp");

        List<ExecutionNode> originals = Arrays.asList(sqlNode, relNode, saveNode, tempNode);
        List<String> expectedNames = Arrays.asList("sql-ENRICHED", "rel-ENRICHED", "save-ENRICHED", "temp-ENRICHED");

        for (int i = 0; i < originals.size(); i++)
        {
            ExecutionNode enriched = harness.invokeSeam(originals.get(i));
            Assert.assertNotSame("Node " + originals.get(i).getClass().getSimpleName() + " should be cloned by the seam",
                    originals.get(i), enriched);

            RelationalDatabaseConnection enrichedConn = extractConnection(enriched);
            Assert.assertEquals("Enriched connection should carry the stamped databaseName for " + originals.get(i).getClass().getSimpleName(),
                    expectedNames.get(i),
                    ((StaticDatasourceSpecification) enrichedConn.datasourceSpecification).databaseName);
        }

        Assert.assertEquals("Preprocessor should have been called exactly once per .connection-bearing node type",
                4, spy.preprocessCount.get());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static RelationalDatabaseConnection buildConnection(String dbName)
    {
        StaticDatasourceSpecification spec = new StaticDatasourceSpecification();
        spec.host = "127.0.0.1";
        spec.port = TEST_PORT;
        spec.databaseName = dbName;

        RelationalDatabaseConnection connection = new RelationalDatabaseConnection();
        connection.datasourceSpecification = spec;
        connection.authenticationStrategy = new TestDatabaseAuthenticationStrategy();
        connection.databaseType = DatabaseType.H2;
        connection.type = DatabaseType.H2;
        return connection;
    }

    @SuppressWarnings("deprecation")
    private static RelationalDatabaseConnection extractConnection(ExecutionNode node)
    {
        if (node instanceof SQLExecutionNode)
        {
            return (RelationalDatabaseConnection) ((SQLExecutionNode) node).connection;
        }
        if (node instanceof RelationalExecutionNode)
        {
            return (RelationalDatabaseConnection) ((RelationalExecutionNode) node).connection;
        }
        if (node instanceof RelationalSaveNode)
        {
            return (RelationalDatabaseConnection) ((RelationalSaveNode) node).connection;
        }
        if (node instanceof CreateAndPopulateTempTableExecutionNode)
        {
            return (RelationalDatabaseConnection) ((CreateAndPopulateTempTableExecutionNode) node).connection;
        }
        throw new IllegalArgumentException("Not a .connection-bearing node type: " + node.getClass());
    }

    /**
     * Wires up a {@link RelationalExecutionNodeExecutor} with a spy preprocessor installed at
     * the head of its {@link ConnectionManagerSelector}'s manager list, then exposes the private
     * {@code preprocessConnectionIfNeeded} seam via reflection.
     */
    private static final class SeamHarness
    {
        private final RelationalExecutionNodeExecutor executor;
        private final Method preprocessSeam;

        private SeamHarness(RelationalExecutionNodeExecutor executor) throws Exception
        {
            this.executor = executor;
            this.preprocessSeam = RelationalExecutionNodeExecutor.class.getDeclaredMethod(
                    "preprocessConnectionIfNeeded", ExecutionNode.class);
            this.preprocessSeam.setAccessible(true);
        }

        @SuppressWarnings("unchecked")
        static SeamHarness withSpy(SpyPreprocessor spy) throws Exception
        {
            RelationalStoreExecutionState relState = new RelationalStoreExecutionState(new RelationalStoreState(TEST_PORT));
            ExecutionState state = new ExecutionState(
                    Maps.mutable.empty(),
                    Lists.mutable.empty(),
                    Lists.mutable.with(relState));

            ConnectionManagerSelector selector = ((RelationalStoreExecutionState) state.getStoreExecutionState(StoreType.Relational))
                    .getRelationalExecutor().getConnectionManager();
            Field managers = ConnectionManagerSelector.class.getDeclaredField("connectionManagers");
            managers.setAccessible(true);
            ((MutableList<ConnectionManager>) managers.get(selector)).add(0, spy);

            return new SeamHarness(new RelationalExecutionNodeExecutor(state, Identity.getAnonymousIdentity()));
        }

        ExecutionNode invokeSeam(ExecutionNode input) throws Exception
        {
            return (ExecutionNode) this.preprocessSeam.invoke(this.executor, input);
        }
    }
}

