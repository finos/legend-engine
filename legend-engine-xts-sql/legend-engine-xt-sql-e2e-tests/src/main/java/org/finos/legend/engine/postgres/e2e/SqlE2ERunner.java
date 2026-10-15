// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.postgres.e2e;

import org.finos.legend.engine.shared.core.vault.PropertiesVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Process-wide, lazily-initialised harness for the SQL e2e parity corpus.
 *
 * <p>Owns the reference PostgreSQL instance (Testcontainers by default), the seeded schema and the
 * parsed YAML corpus. Exposes the corpus - already rewritten per execution path, with the reference
 * result already computed - as typed Java objects ({@link CaseRef}, {@link ConnectionInfo}) that each
 * runtime's Pure extension builds CoreInstances from directly.
 *
 * <p>Knows nothing about plan generation or the Legend SQL server: in the interpreted dev loop the
 * Legend side is driven from Pure so {@code .pure} edits are live. The full-fidelity pg-wire path
 * remains {@link TestPostgresParity}'s job.
 *
 * <p>Initialised on first access and kept for the lifetime of the JVM.
 */
public final class SqlE2ERunner
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SqlE2ERunner.class);

    private static final String IMAGE = "postgres:16-alpine";

    /**
     * Resource root to read the corpus from instead of the classpath, making YAML edits live for a
     * long-running session (the Pure LSP dev loop). Unset in CI, where the packaged corpus is the
     * right one and cannot change mid-run.
     */
    private static final String CORPUS_DIR_PROPERTY = "sql.e2e.corpus.dir";
    private static final long CORPUS_CHECK_INTERVAL_MS = 1000L;

    public static final String[] TEST_FILES = {
            "parity-tests/schema.yaml",
            "parity-tests/smoke_tests.yaml",
            "parity-tests/functions/math_functions.yaml",
            "parity-tests/functions/string_functions.yaml",
            "parity-tests/functions/binary_functions.yaml",
            "parity-tests/functions/pgcrypto_functions.yaml",
            "parity-tests/functions/pattern_matching.yaml",
            "parity-tests/functions/formatting_functions.yaml",
            "parity-tests/functions/datetime_functions.yaml",
            "parity-tests/functions/date_literals.yaml",
            "parity-tests/functions/conditional_functions.yaml",
            "parity-tests/functions/json_functions.yaml",
            "parity-tests/functions/array_functions.yaml",
            "parity-tests/functions/aggregate_functions.yaml",
            "parity-tests/functions/window_functions.yaml",
            "parity-tests/functions/network_functions.yaml",
            "parity-tests/functions/system_functions.yaml",
            "parity-tests/functions/sequence_functions.yaml",
            "parity-tests/functions/set_returning_functions.yaml",
            "parity-tests/operators/math_operators.yaml",
            "parity-tests/operators/string_operators.yaml",
            "parity-tests/operators/comparison_operators.yaml",
            "parity-tests/operators/logical_operators.yaml",
            "parity-tests/operators/pattern_matching_operators.yaml",
            "parity-tests/operators/json_operators.yaml",
            "parity-tests/operators/datetime_operators.yaml",
            "parity-tests/operators/other_type_operators.yaml",
            "parity-tests/operators/bitstring_operators.yaml",
            "parity-tests/operators/array_operators.yaml",
            "parity-tests/operators/range_operators.yaml",
            "parity-tests/operators/network_operators.yaml",
            "parity-tests/operators/fts_operators.yaml",
            "parity-tests/operators/geometric_operators.yaml",
            "parity-tests/predicates/comparison_predicates.yaml",
            "parity-tests/format_tokens/to_char_tokens.yaml",
            "parity-tests/format_tokens/extract_fields.yaml",
            "parity-tests/structural/joins.yaml",
            "parity-tests/structural/set_operations.yaml",
            "parity-tests/structural/subqueries.yaml",
            "parity-tests/structural/ctes.yaml",
            "parity-tests/structural/order_limit_offset.yaml",
            "parity-tests/structural/group_by.yaml",
            "parity-tests/structural/distinct.yaml",
            "parity-tests/structural/null_semantics.yaml",
            "parity-tests/structural/type_casting.yaml",
            "parity-tests/structural/case_expressions.yaml",
            "parity-tests/structural/where_predicates.yaml",
            "parity-tests/structural/aliases.yaml",
            "parity-tests/structural/having.yaml",
            "parity-tests/structural/lateral_joins.yaml",
            "parity-tests/structural/boolean_logic.yaml",
            "parity-tests/structural/select_star.yaml",
            "parity-tests/structural/multiple_schemas.yaml",
            "parity-tests/structural/json_operators.yaml",
            "parity-tests/structural/interval_arithmetic.yaml",
            "parity-tests/structural/column_resolution_across_renames.yaml",
            "parity-tests/structural/column_resolution_corpus_shapes.yaml",
            "parity-tests/structural/grouping_sets.yaml",
            "parity-tests/structural/filter_clause.yaml",
            "parity-tests/structural/within_group.yaml",
            "parity-tests/structural/tablesample.yaml",
            "parity-tests/structural/fetch_with_ties.yaml",
            "parity-tests/structural/recursive_ctes.yaml",
            "parity-tests/structural/values_clause.yaml",
            "parity-tests/window_frames/frame_types.yaml",
            "parity-tests/window_frames/partition_ordering.yaml",
            "parity-tests/window_frames/frame_exclusion.yaml",
            "parity-tests/window_frames/named_windows.yaml",
            "parity-tests/compositions/agg_window_mix.yaml",
            "parity-tests/compositions/multi_join_agg.yaml",
            "parity-tests/compositions/stress_queries.yaml",
            "parity-tests/compositions/nested_subqueries.yaml",
            "parity-tests/compositions/window_over_agg.yaml"
    };

    private static volatile SqlE2ERunner instance;

    private final PostgreSQLContainer<?> container;
    private final PGSimpleDataSource dataSource;
    private final DirectPostgresRunner reference;
    // Swapped wholesale on reload, never mutated in place: executions run concurrently, and
    // clearing-then-repopulating a live map would let a reader observe a half-empty corpus.
    private volatile Map<String, Entry> corpus = new LinkedHashMap<>();
    private volatile Set<String> knownTables = new HashSet<>();
    private final Map<String, ResultMatrix> referenceCache = new ConcurrentHashMap<>();
    private final Map<String, String> referenceErrorCache = new ConcurrentHashMap<>();
    private final Path corpusDir = resolveCorpusDir();
    private volatile long lastCorpusCheckMs;
    private volatile long loadedCorpusStamp;
    private final String host;
    private final int port;
    private final String database;
    private final String user;
    private final String password;

    private static final class Entry
    {
        private final TestCaseLoader.TestCase testCase;
        private final String category;

        private Entry(TestCaseLoader.TestCase testCase, String category)
        {
            this.testCase = testCase;
            this.category = category;
        }
    }

    public static SqlE2ERunner get()
    {
        SqlE2ERunner local = instance;
        if (local == null)
        {
            synchronized (SqlE2ERunner.class)
            {
                local = instance;
                if (local == null)
                {
                    local = new SqlE2ERunner();
                    instance = local;
                    Runtime.getRuntime().addShutdownHook(new Thread(local::shutDown, "sql-e2e-runner-shutdown"));
                }
            }
        }
        return local;
    }

    private SqlE2ERunner()
    {
        long start = System.currentTimeMillis();
        String externalHost = System.getProperty("sql.e2e.postgres.host");
        if (externalHost != null && !externalHost.isEmpty())
        {
            this.container = null;
            this.host = externalHost;
            this.port = Integer.parseInt(System.getProperty("sql.e2e.postgres.port", "5432"));
            this.database = System.getProperty("sql.e2e.postgres.database", "postgres");
            this.user = System.getProperty("sql.e2e.postgres.user", "postgres");
            this.password = System.getProperty("sql.e2e.postgres.password", "postgres");
            LOGGER.info("SQL e2e harness using external Postgres at {}:{}/{}", this.host, this.port, this.database);
        }
        else
        {
            String registry = System.getProperty("legend.engine.testcontainer.registry");
            String image = (registry == null || registry.isEmpty()) ? IMAGE : registry + "/" + IMAGE;
            // a registry-prefixed name is not recognised by Testcontainers' compatibility check
            PostgreSQLContainer<?> pg = new PostgreSQLContainer<>(
                    DockerImageName.parse(image).asCompatibleSubstituteFor("postgres"));
            pg.start();
            this.container = pg;
            this.host = pg.getHost();
            this.port = pg.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT);
            this.database = pg.getDatabaseName();
            this.user = pg.getUsername();
            this.password = pg.getPassword();
            LOGGER.info("SQL e2e harness started Postgres container {} at {}:{}", image, this.host, this.port);
        }

        this.dataSource = new PGSimpleDataSource();
        this.dataSource.setServerNames(new String[]{this.host});
        this.dataSource.setPortNumbers(new int[]{this.port});
        this.dataSource.setDatabaseName(this.database);
        this.dataSource.setUser(this.user);
        this.dataSource.setPassword(this.password);
        this.reference = new DirectPostgresRunner(this.dataSource);

        // the Pure-side runtime authenticates via vault references 'e2e.user'/'e2e.password'
        java.util.Properties vaultProps = new java.util.Properties();
        vaultProps.put("e2e.user", this.user);
        vaultProps.put("e2e.password", this.password);
        Vault.INSTANCE.registerImplementation(new PropertiesVaultImplementation(vaultProps));

        try
        {
            loadCorpusAndSeed();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to initialise SQL e2e harness", e);
        }
        LOGGER.info("SQL e2e harness ready with {} cases in {} ms (corpus: {})", this.corpus.size(),
                System.currentTimeMillis() - start,
                this.corpusDir == null ? "classpath, fixed for this JVM" : this.corpusDir + ", reloaded on edit");
    }

    private void loadCorpusAndSeed() throws Exception
    {
        readCorpus(true);
        try (Connection conn = this.dataSource.getConnection();
             Statement stmt = conn.createStatement())
        {
            stmt.execute("CREATE EXTENSION IF NOT EXISTS pgcrypto");
            stmt.execute("CREATE SEQUENCE IF NOT EXISTS test_seq START 1");
        }
    }

    /**
     * Parses every corpus file and publishes a fresh {@code corpus}/{@code knownTables} pair.
     * {@code createSchemas} is true only on first load: re-running {@link SchemaManager} would drop
     * and recreate tables in the live Postgres, so a reload picks up case edits but NOT schema
     * edits - changing {@code schema.yaml} still needs a restart.
     */
    private void readCorpus(boolean createSchemas) throws Exception
    {
        Map<String, Entry> nextCorpus = new LinkedHashMap<>();
        Set<String> nextTables = new HashSet<>();
        for (String testFile : TEST_FILES)
        {
            TestCaseLoader.TestFile file = TestCaseLoader.load(testFile, this.corpusDir);
            if (file.schema != null)
            {
                if (createSchemas)
                {
                    new SchemaManager(this.dataSource).createSchema(file.schema);
                }
                for (TestCaseLoader.TableDef table : file.schema.tables)
                {
                    nextTables.add(table.name.toLowerCase());
                }
            }
            if (file.tests != null)
            {
                String category = testFile.replace("parity-tests/", "").replace(".yaml", "");
                for (TestCaseLoader.TestCase tc : file.tests)
                {
                    nextCorpus.put(tc.id, new Entry(tc, category));
                }
            }
        }
        this.corpus = nextCorpus;
        this.knownTables = nextTables;
    }

    /**
     * Picks up corpus edits without a restart, when {@code -Dsql.e2e.corpus.dir} points at a source
     * tree. A classpath-loaded corpus lives in a jar that cannot change under a running JVM, so it
     * is read once and this is a no-op.
     * <p>
     * Gated on newest-mtime rather than reloading unconditionally: a full parse is ~80ms, which is
     * negligible per batch but not per case, and {@code executeSQLE2ETest} resolves one case at a
     * time. The time check keeps the stat storm off the hot path in between.
     * <p>
     * Reference caches are deliberately left alone - they are keyed by SQL text, so an edited case
     * misses naturally and re-executes, while entries for removed cases are simply never looked up.
     */
    private void refreshCorpusIfChanged()
    {
        if (this.corpusDir == null)
        {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - this.lastCorpusCheckMs < CORPUS_CHECK_INTERVAL_MS)
        {
            return;
        }
        synchronized (this)
        {
            if (now - this.lastCorpusCheckMs < CORPUS_CHECK_INTERVAL_MS)
            {
                return;
            }
            this.lastCorpusCheckMs = now;
            long stamp = newestCorpusMtime();
            if (stamp == this.loadedCorpusStamp)
            {
                return;
            }
            try
            {
                readCorpus(false);
                this.loadedCorpusStamp = stamp;
                LOGGER.info("SQL e2e corpus reloaded from {} ({} cases)", this.corpusDir, this.corpus.size());
            }
            catch (Exception e)
            {
                // Keep serving the last good corpus: a half-saved YAML edit should surface as a
                // logged warning on the next call, not tear down a running dev-loop session.
                LOGGER.warn("SQL e2e corpus reload failed, keeping the previously loaded corpus", e);
            }
        }
    }

    private long newestCorpusMtime()
    {
        long newest = 0L;
        for (String testFile : TEST_FILES)
        {
            try
            {
                Path p = this.corpusDir.resolve(testFile);
                if (Files.isRegularFile(p))
                {
                    newest = Math.max(newest, Files.getLastModifiedTime(p).toMillis());
                }
            }
            catch (IOException e)
            {
                LOGGER.debug("Could not stat corpus file {}", testFile, e);
            }
        }
        return newest;
    }

    /**
     * Resolves {@code -Dsql.e2e.corpus.dir} to the resource root holding {@code parity-tests/}.
     * Returns null (meaning "read from the classpath") when unset or when it does not point at a
     * usable directory - a bad path degrades to the packaged corpus with a warning rather than
     * failing the session.
     */
    private static Path resolveCorpusDir()
    {
        String configured = System.getProperty(CORPUS_DIR_PROPERTY);
        if (configured == null || configured.trim().isEmpty())
        {
            return null;
        }
        Path dir = Paths.get(configured.trim());
        if (!Files.isDirectory(dir))
        {
            LOGGER.warn("{}={} is not a directory; reading the corpus from the classpath instead",
                    CORPUS_DIR_PROPERTY, configured);
            return null;
        }
        if (!Files.isDirectory(dir.resolve("parity-tests")))
        {
            LOGGER.warn("{}={} has no parity-tests/ subdirectory - it should be the resource root, "
                    + "not the corpus directory itself; reading from the classpath instead",
                    CORPUS_DIR_PROPERTY, configured);
            return null;
        }
        return dir;
    }

    /**
     * Resolve a filter to matching test ids. A filter is an exact id, an exact category
     * ({@code functions/math_functions}), a prefix ending in {@code *}, or empty for everything.
     */
    public List<String> listIds(String filter)
    {
        refreshCorpusIfChanged();
        List<String> out = new ArrayList<>();
        String f = filter == null ? "" : filter.trim();
        for (Map.Entry<String, Entry> e : this.corpus.entrySet())
        {
            if (matches(f, e.getKey(), e.getValue().category))
            {
                out.add(e.getKey());
            }
        }
        return out;
    }

    /**
     * Every matching corpus id crossed with both execution paths, each fully resolved - the atomic
     * unit executeSQLE2ETest/runOneCase operate on. Skipped cases are still listed: the skip itself
     * is part of what gets exercised (reported as SKIP, not silently dropped).
     */
    public List<CaseRef> resolveCaseRefs(String filter)
    {
        List<CaseRef> out = new ArrayList<>();
        for (String id : listIds(filter))
        {
            out.add(resolveCaseRef(id, "TDS"));
            out.add(resolveCaseRef(id, "Relation"));
        }
        return out;
    }

    /**
     * Resolves one (corpus id, path) pair to everything runOneCase needs: the SQL rewritten for that
     * path, the recorded baseline, and the skip/bug/rewrite-error signals, checked in that priority
     * order - a skipped case never triggers a Postgres call or an AST rewrite, and a
     * reference-Postgres failure never triggers a rewrite attempt.
     */
    public CaseRef resolveCaseRef(String corpusId, String path)
    {
        refreshCorpusIfChanged();
        Entry entry = this.corpus.get(corpusId);
        if (entry == null)
        {
            throw new IllegalStateException("Unknown SQL e2e case id: " + corpusId);
        }
        TestCaseLoader.TestCase tc = entry.testCase;
        String expectedStatus = "TDS".equals(path) ? tc.expected_tds_status : tc.expected_rel_status;
        boolean hasOrderBy = tc.sql != null && tc.sql.toUpperCase().contains("ORDER BY");

        if (tc.skip != null)
        {
            return new CaseRef(corpusId, path, null, hasOrderBy, tc.skip, null, null, expectedStatus);
        }

        String bugReason = this.referenceErrorCache.get(tc.sql);
        if (bugReason == null && this.referenceCache.get(tc.sql) == null)
        {
            try
            {
                this.referenceCache.put(tc.sql, this.reference.execute(tc.sql));
            }
            catch (Exception e)
            {
                bugReason = String.valueOf(e.getMessage());
                this.referenceErrorCache.put(tc.sql, bugReason);
            }
        }
        if (bugReason != null)
        {
            return new CaseRef(corpusId, path, null, hasOrderBy, null, bugReason, null, expectedStatus);
        }

        String prefix = "TDS".equals(path) ? "tds" : "rel";
        try
        {
            String rewrittenSql = Boolean.TRUE.equals(tc.join_func)
                    ? "SELECT * FROM func('e2e::" + prefix + "_person_with_dept') ORDER BY \"name\""
                    : new AstFromRewriter(prefix, this.knownTables).rewrite(tc.sql);
            return new CaseRef(corpusId, path, rewrittenSql, hasOrderBy, null, null, null, expectedStatus);
        }
        catch (Exception e)
        {
            return new CaseRef(corpusId, path, null, hasOrderBy, null, null, String.valueOf(e.getMessage()), expectedStatus);
        }
    }

    /**
     * A corpus id crossed with one execution path, fully resolved. {@code path} is "TDS" or
     * "Relation", kept as a plain string since this class has no Pure dependency.
     */
    public static final class CaseRef
    {
        public final String corpusId;
        public final String path;
        /** Rewritten for this path; null if skip, bugReason, or rewriteError is set. */
        public final String sql;
        public final boolean hasOrderBy;
        public final String skip;
        public final String bugReason;
        public final String rewriteError;
        public final String expectedStatus;

        private CaseRef(String corpusId, String path, String sql, boolean hasOrderBy, String skip,
                         String bugReason, String rewriteError, String expectedStatus)
        {
            this.corpusId = corpusId;
            this.path = path;
            this.sql = sql;
            this.hasOrderBy = hasOrderBy;
            this.skip = skip;
            this.bugReason = bugReason;
            this.rewriteError = rewriteError;
            this.expectedStatus = expectedStatus;
        }
    }

    /**
     * Compares Legend's TDS JSON for a corpus case against its cached reference result. Requires
     * resolveCaseRefs/resolveCaseRef to have already run for corpusId, so the reference is cached.
     */
    public String compareToReference(String corpusId, boolean hasOrderBy, String resultJson)
    {
        Entry entry = this.corpus.get(corpusId);
        if (entry == null)
        {
            throw new IllegalStateException("Unknown SQL e2e case id: " + corpusId);
        }
        ResultMatrix reference = this.referenceCache.get(entry.testCase.sql);
        if (reference == null)
        {
            throw new IllegalStateException("No cached reference result for case " + corpusId
                    + " - a referenceError should have been checked before comparing");
        }

        ResultMatrix actual;
        try
        {
            actual = TdsJsonResultMatrix.parse(resultJson);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to parse Legend TDS JSON for case " + corpusId, e);
        }

        ResultMatrix expectedCmp = hasOrderBy ? reference : reference.sorted();
        ResultMatrix actualCmp = hasOrderBy ? actual : actual.sorted();

        ResultComparator.ComparisonResult comparison = ResultComparator.compare(expectedCmp, actualCmp);
        return comparison.isMatch() ? null : String.join("\n", comparison.getDiffs());
    }

    private static boolean matches(String filter, String id, String category)
    {
        if (filter.isEmpty() || "*".equals(filter))
        {
            return true;
        }
        if (filter.endsWith("*"))
        {
            String prefix = filter.substring(0, filter.length() - 1);
            return id.startsWith(prefix) || category.startsWith(prefix);
        }
        return id.equals(filter) || category.equals(filter);
    }

    public ConnectionInfo connectionInfo()
    {
        return new ConnectionInfo(this.host, this.port, this.database, this.user, this.password);
    }

    public static final class ConnectionInfo
    {
        public final String host;
        public final int port;
        public final String database;
        public final String user;
        public final String password;

        private ConnectionInfo(String host, int port, String database, String user, String password)
        {
            this.host = host;
            this.port = port;
            this.database = database;
            this.user = user;
            this.password = password;
        }
    }

    /**
     * Runs the Java-only half of one execution path for ad hoc SQL: FROM-rewrite and reference
     * execution against Postgres. The Legend side itself is run separately via runOneAdhocLegend.
     */
    public AdhocPrep prepareAdhoc(String sql, String path)
    {
        String prefix = "TDS".equals(path) ? "tds" : "rel";

        ResultMatrix reference = null;
        String referenceError = null;
        try
        {
            reference = this.reference.execute(sql);
        }
        catch (Exception e)
        {
            referenceError = String.valueOf(e.getMessage());
        }

        String rewrittenSql = null;
        String rewriteError = null;
        try
        {
            rewrittenSql = new AstFromRewriter(prefix, this.knownTables).rewrite(sql);
        }
        catch (Exception e)
        {
            rewriteError = String.valueOf(e.getMessage());
        }

        boolean hasOrderBy = sql.toUpperCase().contains("ORDER BY");
        return new AdhocPrep(path, prefix, hasOrderBy, rewrittenSql, rewriteError, reference, referenceError);
    }

    public static final class AdhocPrep
    {
        public final String path;
        public final String prefix;
        public final boolean hasOrderBy;
        /** Null if rewriteError is set. */
        public final String rewrittenSql;
        public final String rewriteError;
        /** Null if referenceError is set. */
        public final ResultMatrix reference;
        public final String referenceError;

        private AdhocPrep(String path, String prefix, boolean hasOrderBy, String rewrittenSql, String rewriteError,
                           ResultMatrix reference, String referenceError)
        {
            this.path = path;
            this.prefix = prefix;
            this.hasOrderBy = hasOrderBy;
            this.rewrittenSql = rewrittenSql;
            this.rewriteError = rewriteError;
            this.reference = reference;
            this.referenceError = referenceError;
        }
    }

    public List<String> categories()
    {
        refreshCorpusIfChanged();
        Set<String> out = new java.util.LinkedHashSet<>();
        for (Entry e : this.corpus.values())
        {
            out.add(e.category);
        }
        return Collections.unmodifiableList(new ArrayList<>(out));
    }

    private void shutDown()
    {
        if (this.container != null)
        {
            try
            {
                this.container.stop();
            }
            catch (Exception e)
            {
                LOGGER.debug("Error stopping SQL e2e Postgres container", e);
            }
        }
    }
}
