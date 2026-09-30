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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Covers the harness that backs the interpreted Pure dev loop: corpus load, FROM rewrite for both
 * paths, and reference execution against the seeded Postgres.
 */
public class TestSqlE2ERunner
{
    @Test
    public void listsIdsByCategoryAndExactId()
    {
        SqlE2ERunner runner = SqlE2ERunner.get();
        List<String> smoke = runner.listIds("smoke_tests");
        Assertions.assertFalse(smoke.isEmpty(), "smoke_tests category should resolve to cases");
        Assertions.assertTrue(smoke.contains("smoke_select_literal"), "expected smoke_select_literal in " + smoke);

        Assertions.assertEquals(
                java.util.Collections.singletonList("smoke_select_literal"),
                runner.listIds("smoke_select_literal"));

        Assertions.assertTrue(runner.listIds("").size() > 1000, "empty filter should match the whole corpus");
    }

    @Test
    public void hydratesCaseWithRewritesAndReference()
    {
        SqlE2ERunner runner = SqlE2ERunner.get();

        SqlE2ERunner.ConnectionInfo conn = runner.connectionInfo();
        Assertions.assertTrue(conn.port > 0, "connection port must be set");

        SqlE2ERunner.CaseRef tds = runner.resolveCaseRef("smoke_select_column", "TDS");
        Assertions.assertEquals("smoke_select_column", tds.corpusId);
        Assertions.assertNull(tds.rewriteError, "rewrite should succeed: " + tds.rewriteError);
        Assertions.assertTrue(tds.sql.contains("func('e2e::tds_persons')"),
                "TDS rewrite should target the tds_ accessor, got: " + tds.sql);

        SqlE2ERunner.CaseRef rel = runner.resolveCaseRef("smoke_select_column", "Relation");
        Assertions.assertTrue(rel.sql.contains("func('e2e::rel_persons')"),
                "Relation rewrite should target the rel_ accessor, got: " + rel.sql);
    }

    @Test
    public void tdsJsonResultMatrixParsesTypedValuesIncludingNulls() throws Exception
    {
        String tdsJson = "{"
                + "\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":["
                + "{\"name\":\"id\",\"type\":\"Integer\"},{\"name\":\"name\",\"type\":\"String\"}"
                + "]},"
                + "\"activities\":[],"
                + "\"result\":{\"columns\":[\"id\",\"name\"],\"rows\":["
                + "{\"values\":[1,\"Alice\"]},"
                + "{\"values\":[2,null]}"
                + "]}}";

        ResultMatrix matrix = TdsJsonResultMatrix.parse(tdsJson);
        Assertions.assertEquals(java.util.Arrays.asList("id", "name"), matrix.getColumnNames());
        Assertions.assertEquals(2, matrix.getRowCount());
        Assertions.assertEquals(1L, matrix.getRows().get(0).get(0));
        Assertions.assertEquals("Alice", matrix.getRows().get(0).get(1));
        Assertions.assertNull(matrix.getRows().get(1).get(1), "SQL NULL must parse to a real Java null");
    }

    @Test
    public void resolveCaseRefsCrossesBothPaths()
    {
        SqlE2ERunner runner = SqlE2ERunner.get();
        List<SqlE2ERunner.CaseRef> refs = runner.resolveCaseRefs("smoke_select_literal");
        Assertions.assertEquals(2, refs.size());
        Assertions.assertEquals("smoke_select_literal", refs.get(0).corpusId);
        Assertions.assertEquals("TDS", refs.get(0).path);
        Assertions.assertNotNull(refs.get(0).sql, "a non-skipped, non-bug case must have rewritten sql");
        Assertions.assertEquals("smoke_select_literal", refs.get(1).corpusId);
        Assertions.assertEquals("Relation", refs.get(1).path);
    }

    @Test
    public void resolveCaseRefReportsSkipWithoutTouchingPostgresOrRewrite()
    {
        // abbrev__cidr__unsupported has skip: "unsupported type category" - resolveCaseRef must
        // short-circuit before rewrite/reference execution
        SqlE2ERunner.CaseRef ref = SqlE2ERunner.get().resolveCaseRef("abbrev__cidr__unsupported", "TDS");
        Assertions.assertEquals("unsupported type category", ref.skip);
        Assertions.assertNull(ref.sql);
        Assertions.assertNull(ref.bugReason);
        Assertions.assertNull(ref.rewriteError);
    }

    @Test
    public void resolveCaseRefResolvesExpectedStatusPerPath()
    {
        SqlE2ERunner.CaseRef tds = SqlE2ERunner.get().resolveCaseRef("smoke_select_column", "TDS");
        SqlE2ERunner.CaseRef rel = SqlE2ERunner.get().resolveCaseRef("smoke_select_column", "Relation");
        Assertions.assertEquals("PASS", tds.expectedStatus);
        Assertions.assertEquals("PASS", rel.expectedStatus);
    }

    @Test
    public void compareToReferenceDetectsRowCountMismatch() throws Exception
    {
        SqlE2ERunner runner = SqlE2ERunner.get();
        // populates the reference cache for this case's SQL as a side effect
        runner.resolveCaseRef("smoke_select_column", "TDS");

        String emptyResultJson = "{"
                + "\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":["
                + "{\"name\":\"id\",\"type\":\"Integer\"},{\"name\":\"name\",\"type\":\"String\"}"
                + "]},"
                + "\"activities\":[],"
                + "\"result\":{\"columns\":[\"id\",\"name\"],\"rows\":[]}}";

        String diff = runner.compareToReference("smoke_select_column", true, emptyResultJson);
        Assertions.assertNotNull(diff, "an empty actual result must not match the seeded persons table");
        Assertions.assertTrue(diff.toLowerCase().contains("row count"), diff);
    }

    @Test
    public void corpusDirUnsetMeansClasspathAndAFixedCorpus()
    {
        // The default: no property, corpus comes from the jar and cannot change under the JVM.
        Assertions.assertNull(System.getProperty("sql.e2e.corpus.dir"),
                "this test asserts the default; something set the property");
        Assertions.assertTrue(SqlE2ERunner.get().listIds("").size() > 1000);
    }

    @Test
    public void loadsCorpusFromADirectoryWhenGivenOne() throws Exception
    {
        // Same relative paths as the classpath form - sql.e2e.corpus.dir is the resource ROOT, the
        // directory holding parity-tests/, so TEST_FILES needs no second spelling.
        java.nio.file.Path root = java.nio.file.Paths.get("src/main/resources");
        Assertions.assertTrue(java.nio.file.Files.isDirectory(root.resolve("parity-tests")),
                "expected to run from the module dir; got " + root.toAbsolutePath());

        TestCaseLoader.TestFile fromDir = TestCaseLoader.load("parity-tests/smoke_tests.yaml", root);
        TestCaseLoader.TestFile fromCp = TestCaseLoader.load("parity-tests/smoke_tests.yaml");
        Assertions.assertEquals(fromCp.tests.size(), fromDir.tests.size());
        Assertions.assertEquals(fromCp.tests.get(0).id, fromDir.tests.get(0).id);
    }

    @Test
    public void aMissingFileUnderTheCorpusDirIsReportedWithThatDir()
    {
        java.nio.file.Path root = java.nio.file.Paths.get("src/main/resources");
        RuntimeException e = Assertions.assertThrows(RuntimeException.class,
                () -> TestCaseLoader.load("parity-tests/no_such_file.yaml", root));
        Assertions.assertTrue(e.getMessage().contains("no_such_file.yaml"), e.getMessage());
        Assertions.assertTrue(e.getMessage().contains("src/main/resources"), e.getMessage());
    }
}
