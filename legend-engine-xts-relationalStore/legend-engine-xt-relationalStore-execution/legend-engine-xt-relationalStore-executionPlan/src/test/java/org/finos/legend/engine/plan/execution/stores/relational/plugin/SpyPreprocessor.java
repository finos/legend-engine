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

import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManager;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;

/**
 * Test-scoped {@link ConnectionManager} that instruments every call to
 * {@link #preprocessConnection(DatabaseConnection, Identity, Map)} — records the input, records
 * (and applies) a caller-supplied rewrite, and counts total invocations.  All other
 * {@code ConnectionManager} SPI methods return {@code null} so this spy can be added at the head
 * of {@link org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector}'s
 * manager list without displacing the real underlying manager for any other concern.
 * <p>
 * Multiple factory helpers cover the two rewrites we need across tests:
 * <ul>
 *   <li>{@link #stampDatabaseName(String)} — appends a marker suffix to {@code datasourceSpecification.databaseName}</li>
 *   <li>{@link #flipType(DatabaseType)} — replaces the connection's {@code type} field</li>
 * </ul>
 * Both produce a NEW {@link RelationalDatabaseConnection} instance so the invariant
 * "no-mutation of input" is preserved by the spy itself.
 */
public class SpyPreprocessor implements ConnectionManager
{
    public final AtomicInteger preprocessCount = new AtomicInteger(0);
    public final List<DatabaseConnection> observedInputs = Collections.synchronizedList(new ArrayList<>());
    public final List<DatabaseConnection> producedOutputs = Collections.synchronizedList(new ArrayList<>());
    public final Map<String, DatabaseConnection> perThreadObservedInput = new ConcurrentHashMap<>();
    public final Map<String, DatabaseConnection> perThreadProducedOutput = new ConcurrentHashMap<>();

    private final UnaryOperator<RelationalDatabaseConnection> rewriter;

    public SpyPreprocessor(UnaryOperator<RelationalDatabaseConnection> rewriter)
    {
        this.rewriter = rewriter;
    }

    @Override
    public DataSourceSpecification getDataSourceSpecification(DatabaseConnection databaseConnection)
    {
        return null;
    }

    @Override
    public ConnectionKey generateKeyFromDatabaseConnection(DatabaseConnection databaseConnection)
    {
        return null;
    }

    @Override
    public Connection getTestDatabaseConnection()
    {
        return null;
    }

    @Override
    public DatabaseConnection preprocessConnection(DatabaseConnection connection, Identity identity, Map<String, Result> allocationResults)
    {
        this.preprocessCount.incrementAndGet();
        this.observedInputs.add(connection);
        String threadName = Thread.currentThread().getName();
        this.perThreadObservedInput.put(threadName, connection);
        DatabaseConnection out = (connection instanceof RelationalDatabaseConnection)
                ? this.rewriter.apply((RelationalDatabaseConnection) connection)
                : connection;
        this.producedOutputs.add(out);
        this.perThreadProducedOutput.put(threadName, out);
        return out;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Factory helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Appends {@code "-" + suffix} to {@code datasourceSpecification.databaseName} (or leaves the
     * connection unchanged if the spec is not a {@link StaticDatasourceSpecification}).  The
     * returned instance is a shallow copy — original input is never mutated.
     */
    public static SpyPreprocessor stampDatabaseName(String suffix)
    {
        return new SpyPreprocessor(orig ->
        {
            if (!(orig.datasourceSpecification instanceof StaticDatasourceSpecification))
            {
                return orig;
            }
            StaticDatasourceSpecification origSpec = (StaticDatasourceSpecification) orig.datasourceSpecification;
            StaticDatasourceSpecification enrichedSpec = new StaticDatasourceSpecification();
            enrichedSpec.host = origSpec.host;
            enrichedSpec.port = origSpec.port;
            enrichedSpec.databaseName = origSpec.databaseName + "-" + suffix;

            RelationalDatabaseConnection enriched = new RelationalDatabaseConnection();
            enriched.datasourceSpecification = enrichedSpec;
            enriched.authenticationStrategy = orig.authenticationStrategy;
            enriched.databaseType = orig.databaseType;
            enriched.type = orig.type;
            enriched.timeZone = orig.timeZone;
            enriched.quoteIdentifiers = orig.quoteIdentifiers;
            return enriched;
        });
    }

    /**
     * Replaces the connection's {@code type} field.  The only knob
     * {@link org.finos.legend.engine.plan.execution.stores.relational.result.DatabaseIdentifiersCaseSensitiveVisitor}
     * short-circuits on — flipping {@code Redshift} vs. anything-else deterministically changes
     * the case-sensitivity decision.
     * <p>
     * <b>Note:</b> flipping {@code type} to a value the executor doesn't have a transformer for
     * (e.g. Redshift in a test module that only wires H2) will break JDBC acquisition.  Prefer
     * {@link #stampTimeZone(String)} for end-to-end tests that must actually connect.  This
     * factory is intended for surgical tests that stop at the seam.
     */
    public static SpyPreprocessor flipType(DatabaseType newType)
    {
        return new SpyPreprocessor(orig ->
        {
            RelationalDatabaseConnection enriched = new RelationalDatabaseConnection();
            enriched.datasourceSpecification = orig.datasourceSpecification;
            enriched.authenticationStrategy = orig.authenticationStrategy;
            enriched.databaseType = newType;
            enriched.type = newType;
            enriched.timeZone = orig.timeZone;
            enriched.quoteIdentifiers = orig.quoteIdentifiers;
            return enriched;
        });
    }

    /**
     * Overwrites the connection's {@code timeZone} field with a marker value.  The
     * {@code timeZone} field is read by result-processing (via
     * {@link org.finos.legend.engine.plan.execution.stores.relational.RelationalExecutor}
     * → {@code SQLExecutionResult}) but does NOT influence JDBC driver selection or the
     * connection URL.  This makes it a safe JDBC-neutral observable for end-to-end tests.
     */
    public static SpyPreprocessor stampTimeZone(String marker)
    {
        return new SpyPreprocessor(orig ->
        {
            RelationalDatabaseConnection enriched = new RelationalDatabaseConnection();
            enriched.datasourceSpecification = orig.datasourceSpecification;
            enriched.authenticationStrategy = orig.authenticationStrategy;
            enriched.databaseType = orig.databaseType;
            enriched.type = orig.type;
            enriched.timeZone = marker;
            enriched.quoteIdentifiers = orig.quoteIdentifiers;
            return enriched;
        });
    }

    /**
     * No rewrite — returns the input reference unchanged.  Useful for counter-only tests where
     * we only care about invocation counts, not enrichment.
     */
    public static SpyPreprocessor passthrough()
    {
        return new SpyPreprocessor(orig -> orig);
    }
}


