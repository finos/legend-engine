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

package org.finos.legend.engine.postgres.e2e.coverage.site;

import org.finos.legend.engine.postgres.e2e.TestCaseLoader;
import org.finos.legend.engine.postgres.e2e.coverage.FunctionCatalogExtractor;
import org.finos.legend.engine.postgres.e2e.coverage.OperatorCatalogExtractor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Golden test for {@link DocumentationSiteGenerator}: exercises it against a tiny synthetic
 * {@code parity-report.json} + catalog fixture (no live Postgres/Legend server needed) and
 * asserts on the presence of key content in the generated HTML files, per the deliverables
 * checklist in {@code docs/parity-site-plan.md} &sect;9.
 */
public class TestDocumentationSiteGenerator
{
    @Test
    void generatesNavigableSiteFromSyntheticData(@TempDir File tempDir) throws Exception
    {
        // --- Synthetic function catalog: one tested, one untested function ---
        Map<String, List<FunctionCatalogExtractor.PgFunction>> functionCatalog = new LinkedHashMap<>();
        FunctionCatalogExtractor.PgFunction lower = new FunctionCatalogExtractor.PgFunction(
                "lower", "lower(text) \u2192 text", "text", "text", "f", FunctionCatalogExtractor.CAT_STRING);
        FunctionCatalogExtractor.PgFunction upperUntested = new FunctionCatalogExtractor.PgFunction(
                "upper", "upper(text) \u2192 text", "text", "text", "f", FunctionCatalogExtractor.CAT_STRING);
        functionCatalog.put(FunctionCatalogExtractor.CAT_STRING, new ArrayList<>(Arrays.asList(lower, upperUntested)));

        // --- Synthetic operator catalog: one tested operator ---
        Map<String, List<OperatorCatalogExtractor.PgOperator>> operatorCatalog = new LinkedHashMap<>();
        OperatorCatalogExtractor.PgOperator plus = new OperatorCatalogExtractor.PgOperator(
                "+", "int4 + int4 \u2192 int4", "int4", "int4", "int4", "b", OperatorCatalogExtractor.CAT_MATH);
        operatorCatalog.put(OperatorCatalogExtractor.CAT_MATH, new ArrayList<>(Arrays.asList(plus)));

        // --- Synthetic test cases: one function test, one operator test, one structural test ---
        TestCaseLoader.TestCase lowerTest = new TestCaseLoader.TestCase();
        lowerTest.id = "lower__basic";
        lowerTest.sql = "SELECT lower('ABC')";
        lowerTest.function = "lower";
        lowerTest.signature = "lower(text) \u2192 text";
        lowerTest.expected_tds_status = "PASS";
        lowerTest.expected_rel_status = "PASS";

        TestCaseLoader.TestCase plusTest = new TestCaseLoader.TestCase();
        plusTest.id = "plus__int__basic";
        plusTest.sql = "SELECT 1 + 1";
        plusTest.operator = "+";
        plusTest.signature = "int4 + int4 \u2192 int4";
        plusTest.expected_tds_status = "PASS";
        plusTest.expected_rel_status = "FAIL";

        TestCaseLoader.TestCase joinTest = new TestCaseLoader.TestCase();
        joinTest.id = "struct_inner_join";
        joinTest.sql = "SELECT * FROM a JOIN b ON a.id = b.id";
        joinTest.feature = "Inner join";
        joinTest.category = "joins";
        joinTest.expected_tds_status = "PASS";
        joinTest.expected_rel_status = "PASS";

        TestCaseLoader.TestCase errTest = new TestCaseLoader.TestCase();
        errTest.id = "coalesce__int__first_non_null";
        errTest.sql = "SELECT coalesce(NULL, 1)";
        errTest.function = "coalesce";
        errTest.signature = "coalesce(int, int) \u2192 int";
        errTest.expected_tds_status = "ERROR";
        errTest.expected_rel_status = "ERROR";

        List<TestCaseLoader.TestCase> allTestCases = Arrays.asList(lowerTest, plusTest, joinTest, errTest);

        // --- Synthetic parity-report.json ---
        File reportFile = new File(tempDir, "parity-report.json");
        try (FileWriter fw = new FileWriter(reportFile))
        {
            fw.write("{\n"
                    + "  \"results\": [\n"
                    + "    {\"id\": \"lower__basic\", \"path\": \"TDS\", \"state\": \"PASS\", \"sql\": \"SELECT lower('ABC')\", \"rewrittenSql\": \"SELECT lower('ABC')\"},\n"
                    + "    {\"id\": \"lower__basic\", \"path\": \"Relation\", \"state\": \"PASS\", \"sql\": \"SELECT lower('ABC')\"},\n"
                    + "    {\"id\": \"plus__int__basic\", \"path\": \"TDS\", \"state\": \"PASS\", \"sql\": \"SELECT 1 + 1\"},\n"
                    + "    {\"id\": \"plus__int__basic\", \"path\": \"Relation\", \"state\": \"FAIL\", \"sql\": \"SELECT 1 + 1\"},\n"
                    + "    {\"id\": \"struct_inner_join\", \"path\": \"TDS\", \"state\": \"PASS\", \"sql\": \"SELECT * FROM a JOIN b ON a.id = b.id\"},\n"
                    + "    {\"id\": \"struct_inner_join\", \"path\": \"Relation\", \"state\": \"PASS\", \"sql\": \"SELECT * FROM a JOIN b ON a.id = b.id\"},\n"
                    + "    {\"id\": \"coalesce__int__first_non_null\", \"path\": \"TDS\", \"state\": \"ERROR\", \"sql\": \"SELECT coalesce(NULL, 1)\"},\n"
                    + "    {\"id\": \"coalesce__int__first_non_null\", \"path\": \"Relation\", \"state\": \"ERROR\", \"sql\": \"SELECT coalesce(NULL, 1)\"}\n"
                    + "  ],\n"
                    + "  \"failures\": [\n"
                    + "    {\"id\": \"plus__int__basic\", \"path\": \"Relation\", \"state\": \"FAIL\", \"sql\": \"SELECT 1 + 1\", "
                    + "\"diffs\": [\"row 0 col total: expected <2> but was <3>\"]},\n"
                    + "    {\"id\": \"coalesce__int__first_non_null\", \"path\": \"TDS\", \"state\": \"ERROR\", \"sql\": \"SELECT coalesce(NULL, 1)\", "
                    + "\"rewrittenSql\": \"SELECT coalesce(NULL, 1) FROM tds_dual\", \"error\": \"No function matches the given name 'coalesce'\"},\n"
                    + "    {\"id\": \"coalesce__int__first_non_null\", \"path\": \"Relation\", \"state\": \"ERROR\", \"sql\": \"SELECT coalesce(NULL, 1)\", "
                    + "\"rewrittenSql\": \"SELECT coalesce(NULL, 1) FROM rel_dual\", \"error\": \"No function matches the given name 'coalesce'\"}\n"
                    + "  ]\n"
                    + "}\n");
        }

        String outputDir = new File(tempDir, "site").getAbsolutePath();
        new DocumentationSiteGenerator().generate(
                functionCatalog,
                operatorCatalog,
                allTestCases,
                reportFile,
                null, // exercise the JSON-fallback path (no in-memory ParityReport.TestResult)
                "test-revision",
                outputDir);

        // --- Assets ---
        assertTrue(new File(outputDir, "assets/app.css").isFile(), "app.css should be copied");
        assertTrue(new File(outputDir, "assets/app.js").isFile(), "app.js should be copied");

        // --- Index page: master-detail shell (sidebar + iframe) ---
        String index = read(new File(outputDir, "index.html"));
        assertTrue(index.contains("Legend SQL"), "index should have the site title");
        assertTrue(index.contains("class=\"master-detail\""), "index should render the master-detail layout");
        assertTrue(index.contains("id=\"detail-frame\""), "index should have a detail iframe");
        assertTrue(index.contains("src=\"overview.html\""), "detail iframe should default to the overview page");
        assertTrue(index.contains("nav-area-title\">Functions"), "sidebar should have a Functions area group");
        assertTrue(index.contains("nav-area-title\">Operators"), "sidebar should have an Operators area group");
        assertTrue(index.contains("nav-area-title\">Structural"), "sidebar should have a Structural area group");
        assertTrue(index.contains("href=\"functions/string.html\" target=\"detail-frame\""), "sidebar should link category pages into the detail iframe");

        // --- Overview page: scorecard + full category table (formerly index.html's content) ---
        String overview = read(new File(outputDir, "overview.html"));
        assertTrue(overview.contains("test-revision"), "overview should show the legend revision");
        assertTrue(overview.contains("functions/string.html"), "overview should link to the string functions category");
        assertTrue(overview.contains("operators/math.html"), "overview should link to the math operators category");
        assertTrue(overview.contains("structural/joins.html"), "overview should link to the joins structural category");

        // --- Function category page ---
        String stringFns = read(new File(outputDir, "functions/string.html"));
        assertTrue(stringFns.contains("lower"), "should list the lower() function");
        assertTrue(stringFns.contains("upper"), "should list the untested upper() function");
        assertTrue(stringFns.contains("data-untested=\"true\""), "untested entries should be tagged for the toggle");
        assertTrue(stringFns.contains("test/lower__basic.html"), "tested entry should link to its permalink page");
        assertTrue(stringFns.contains("postgresql.org/docs"), "category page should link back to the PG docs");

        // --- Operator category page ---
        String mathOps = read(new File(outputDir, "operators/math.html"));
        assertTrue(mathOps.contains("test/plus__int__basic.html"));

        // --- Structural category page ---
        String joins = read(new File(outputDir, "structural/joins.html"));
        assertTrue(joins.contains("Inner join"));
        assertTrue(joins.contains("test/struct_inner_join.html"));
        assertTrue(joins.contains("postgresql.org/docs"), "structural category page should link back to the PG docs");

        // --- Per-test permalink pages ---
        String lowerPage = read(new File(outputDir, "test/lower__basic.html"));
        assertTrue(lowerPage.contains("lower__basic"));
        assertTrue(lowerPage.contains("SELECT lower(&#39;ABC&#39;)") || lowerPage.contains("SELECT lower('ABC')"));

        String plusPage = read(new File(outputDir, "test/plus__int__basic.html"));
        assertTrue(plusPage.contains("Differences"), "FAIL path should show a Differences section");
        assertTrue(plusPage.contains("expected"), "FAIL path should surface the captured diff text");
        assertFalse(plusPage.isEmpty());

        // --- ERROR-state test page: SQL, rewritten Legend SQL, and the error message ---
        String errPage = read(new File(outputDir, "test/coalesce__int__first_non_null.html"));
        assertTrue(errPage.contains("SELECT coalesce(NULL, 1)"), "ERROR page should show the input SQL");
        assertTrue(errPage.contains("tds_dual") && errPage.contains("rel_dual"), "ERROR page should show the rewritten Legend SQL for both paths");
        assertTrue(errPage.contains("No function matches the given name"), "ERROR page should surface the error message");
        assertFalse(errPage.contains("Differences"), "ERROR path has no result-set diff, so no Differences section should render");
    }

    private static String read(File f) throws Exception
    {
        assertTrue(f.isFile(), "expected generated file: " + f);
        return new String(Files.readAllBytes(f.toPath()));
    }
}





