// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.postgres.e2e.coverage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.finos.legend.engine.postgres.e2e.TestCaseLoader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates a structural parity report tracking SQL construct coverage
 * (JOINs, UNIONs, subqueries, window frames, compositions, etc.).
 * Produces structural-parity.json and structural-parity.md.
 */
public class StructuralParityReport
{
    private static final Logger LOGGER = LoggerFactory.getLogger(StructuralParityReport.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Per-test result for both paths.
     */
    public static class TestEntry
    {
        public final String testId;
        public String tdsState;
        public String relationState;

        public TestEntry(String testId)
        {
            this.testId = testId;
        }
    }

    /**
     * Aggregated coverage for a single structural feature.
     */
    public static class FeatureCoverage
    {
        public final String featureName;
        public final String categoryName;
        public int total;
        public int tdsPass;
        public int tdsFail;
        public int tdsError;
        public int tdsSkip;
        public int relPass;
        public int relFail;
        public int relError;
        public int relSkip;
        public final List<TestEntry> tests = new ArrayList<>();

        public FeatureCoverage(String featureName, String categoryName)
        {
            this.featureName = featureName;
            this.categoryName = categoryName;
        }

        public String tdsStatus()
        {
            int effective = total - tdsSkip;
            if (effective == 0)
            {
                return "UNTESTED";
            }
            if (tdsPass == effective)
            {
                return "PASS";
            }
            if (tdsPass > 0)
            {
                return "PARTIAL";
            }
            if (tdsFail > 0)
            {
                return "FAIL";
            }
            return "ERROR";
        }

        public String relStatus()
        {
            int effective = total - relSkip;
            if (effective == 0)
            {
                return "UNTESTED";
            }
            if (relPass == effective)
            {
                return "PASS";
            }
            if (relPass > 0)
            {
                return "PARTIAL";
            }
            if (relFail > 0)
            {
                return "FAIL";
            }
            return "ERROR";
        }
    }

    /**
     * Build the report from test cases and results.
     */
    public void generate(List<TestCaseLoader.TestCase> allTestCases, File reportFile, String outputDir) throws IOException
    {
        // Step 1: Collect tests with feature+category, group by feature
        Map<String, TestEntry> testEntryMap = new HashMap<>();
        // category -> feature -> FeatureCoverage
        Map<String, Map<String, FeatureCoverage>> categories = new LinkedHashMap<>();

        for (TestCaseLoader.TestCase tc : allTestCases)
        {
            if (tc.feature != null && tc.category != null)
            {
                TestEntry entry = new TestEntry(tc.id);
                testEntryMap.put(tc.id, entry);

                FeatureCoverage fc = categories
                        .computeIfAbsent(tc.category, k -> new LinkedHashMap<>())
                        .computeIfAbsent(tc.feature, k -> new FeatureCoverage(tc.feature, tc.category));
                fc.total++;
                fc.tests.add(entry);

                if (tc.skip != null)
                {
                    entry.tdsState = "SKIP";
                    entry.relationState = "SKIP";
                    fc.tdsSkip++;
                    fc.relSkip++;
                }
            }
        }

        if (categories.isEmpty())
        {
            return; // no structural tests
        }

        // Step 2: Read results from parity-report.json
        if (reportFile.exists())
        {
            JsonNode report = MAPPER.readTree(reportFile);
            JsonNode results = report.get("results");
            if (results != null)
            {
                for (JsonNode result : results)
                {
                    String id = result.has("id") ? result.get("id").asText() : "";
                    String state = result.has("state") ? result.get("state").asText() : "";
                    String path = result.has("path") ? result.get("path").asText() : "";

                    TestEntry entry = testEntryMap.get(id);
                    if (entry != null)
                    {
                        if ("TDS".equals(path) && entry.tdsState == null)
                        {
                            entry.tdsState = state;
                        }
                        else if ("Relation".equals(path) && entry.relationState == null)
                        {
                            entry.relationState = state;
                        }
                    }
                }
            }
        }

        // Step 3: Tally per feature
        for (Map<String, FeatureCoverage> featureMap : categories.values())
        {
            for (FeatureCoverage fc : featureMap.values())
            {
                fc.tdsPass = fc.tdsFail = fc.tdsError = fc.tdsSkip = 0;
                fc.relPass = fc.relFail = fc.relError = fc.relSkip = 0;
                for (TestEntry e : fc.tests)
                {
                    tallyState(e.tdsState, fc, true);
                    tallyState(e.relationState, fc, false);
                }
            }
        }

        // Step 4: Generate outputs
        Map<String, FailureInfo> failureMap = loadFailures(reportFile);
        new File(outputDir).mkdirs();
        generateJson(categories, outputDir + "/structural-parity.json");
        generateMarkdown(categories, outputDir + "/structural-parity.md", failureMap);
        printConsoleSummary(categories);
    }

    private static Map<String, FailureInfo> loadFailures(File reportFile)
    {
        Map<String, FailureInfo> map = new LinkedHashMap<>();
        if (reportFile == null || !reportFile.exists())
        {
            return map;
        }
        try
        {
            JsonNode report = MAPPER.readTree(reportFile);
            JsonNode failures = report.get("failures");
            if (failures != null)
            {
                for (JsonNode f : failures)
                {
                    String id = f.has("id") ? f.get("id").asText() : "";
                    String pathVal = f.has("path") ? f.get("path").asText() : "";
                    String state = f.has("state") ? f.get("state").asText() : "";
                    String error = f.has("error") ? f.get("error").asText() : "";
                    String sql = f.has("sql") ? f.get("sql").asText() : "";
                    String category = ErrorCategorizer.categorize(state, error);
                    map.put(id + "|" + pathVal, new FailureInfo(id, pathVal, state, sql, error, category));
                }
            }
        }
        catch (IOException e)
        {
            // Silently skip if can't read
        }
        return map;
    }

    private void tallyState(String state, FeatureCoverage fc, boolean isTds)
    {
        if (state == null)
        {
            if (isTds)
            {
                fc.tdsSkip++;
            }
            else
            {
                fc.relSkip++;
            }
            return;
        }
        if (isTds)
        {
            switch (state)
            {
                case "PASS":
                    fc.tdsPass++;
                    break;
                case "FAIL":
                    fc.tdsFail++;
                    break;
                case "ERROR":
                case "BUG":
                    fc.tdsError++;
                    break;
                default:
                    fc.tdsSkip++;
                    break;
            }
        }
        else
        {
            switch (state)
            {
                case "PASS":
                    fc.relPass++;
                    break;
                case "FAIL":
                    fc.relFail++;
                    break;
                case "ERROR":
                case "BUG":
                    fc.relError++;
                    break;
                default:
                    fc.relSkip++;
                    break;
            }
        }
    }

    private static int[] countFeatureStatuses(Iterable<FeatureCoverage> features, boolean tds)
    {
        // [pass, partial, fail, error, untested]
        int[] c = new int[5];
        for (FeatureCoverage fc : features)
        {
            String s = tds ? fc.tdsStatus() : fc.relStatus();
            switch (s)
            {
                case "PASS":
                    c[0]++;
                    break;
                case "PARTIAL":
                    c[1]++;
                    break;
                case "FAIL":
                    c[2]++;
                    break;
                case "ERROR":
                    c[3]++;
                    break;
                default:
                    c[4]++;
                    break;
            }
        }
        return c;
    }

    private void generateJson(Map<String, Map<String, FeatureCoverage>> categories, String path) throws IOException
    {
        ObjectNode root = MAPPER.createObjectNode();

        List<FeatureCoverage> allFeatures = new ArrayList<>();
        int totalTests = 0;
        for (Map<String, FeatureCoverage> fm : categories.values())
        {
            allFeatures.addAll(fm.values());
            for (FeatureCoverage fc : fm.values())
            {
                totalTests += fc.total;
            }
        }

        int[] tds = countFeatureStatuses(allFeatures, true);
        int[] rel = countFeatureStatuses(allFeatures, false);

        ObjectNode summary = root.putObject("summary");
        summary.put("total_features", allFeatures.size());
        summary.put("total_tests", totalTests);
        putCounts(summary.putObject("tds"), tds);
        putCounts(summary.putObject("relation"), rel);

        ArrayNode cats = root.putArray("categories");
        for (Map.Entry<String, Map<String, FeatureCoverage>> entry : categories.entrySet())
        {
            ObjectNode catNode = cats.addObject();
            catNode.put("name", entry.getKey());
            ArrayNode features = catNode.putArray("features");
            for (FeatureCoverage fc : entry.getValue().values())
            {
                ObjectNode fnNode = features.addObject();
                fnNode.put("name", fc.featureName);

                ObjectNode tdsNode = fnNode.putObject("tds");
                tdsNode.put("status", fc.tdsStatus());
                tdsNode.put("pass", fc.tdsPass);
                tdsNode.put("fail", fc.tdsFail);
                tdsNode.put("error", fc.tdsError);
                tdsNode.put("total", fc.total);

                ObjectNode relNode = fnNode.putObject("relation");
                relNode.put("status", fc.relStatus());
                relNode.put("pass", fc.relPass);
                relNode.put("fail", fc.relFail);
                relNode.put("error", fc.relError);
                relNode.put("total", fc.total);

                ArrayNode details = fnNode.putArray("testDetails");
                for (TestEntry te : fc.tests)
                {
                    ObjectNode d = details.addObject();
                    d.put("id", te.testId);
                    d.put("tds", te.tdsState != null ? te.tdsState : "UNKNOWN");
                    d.put("relation", te.relationState != null ? te.relationState : "UNKNOWN");
                }
            }
        }

        MAPPER.writeValue(new File(path), root);
    }

    private static void putCounts(ObjectNode node, int[] c)
    {
        node.put("pass", c[0]);
        node.put("partial", c[1]);
        node.put("fail", c[2]);
        node.put("error", c[3]);
        node.put("untested", c[4]);
    }

    private void generateMarkdown(Map<String, Map<String, FeatureCoverage>> categories, String path, Map<String, FailureInfo> failureMap) throws IOException
    {
        StringBuilder md = new StringBuilder();
        md.append("# SQL Structural Parity — Legend SQL (LegendSql)\n\n");

        List<FeatureCoverage> allFeatures = new ArrayList<>();
        int totalTests = 0;
        int totalTdsPass = 0;
        int totalTdsFail = 0;
        int totalTdsError = 0;
        int totalTdsSkip = 0;
        int totalRelPass = 0;
        int totalRelFail = 0;
        int totalRelError = 0;
        int totalRelSkip = 0;
        for (Map<String, FeatureCoverage> fm : categories.values())
        {
            allFeatures.addAll(fm.values());
            for (FeatureCoverage fc : fm.values())
            {
                totalTests += fc.total;
                totalTdsPass += fc.tdsPass;
                totalTdsFail += fc.tdsFail;
                totalTdsError += fc.tdsError;
                totalTdsSkip += fc.tdsSkip;
                totalRelPass += fc.relPass;
                totalRelFail += fc.relFail;
                totalRelError += fc.relError;
                totalRelSkip += fc.relSkip;
            }
        }

        int[] tds = countFeatureStatuses(allFeatures, true);
        int[] rel = countFeatureStatuses(allFeatures, false);

        md.append("## Summary\n\n");
        md.append("| Metric | TDS | Relation |\n|--------|-----|----------|\n");
        md.append(String.format("| Total features | %d | %d |\n", allFeatures.size(), allFeatures.size()));
        md.append(String.format("| Total tests | %d | %d |\n", totalTests, totalTests));
        md.append(String.format("| PASS | %d | %d |\n", totalTdsPass, totalRelPass));
        md.append(String.format("| FAIL | %d | %d |\n", totalTdsFail, totalRelFail));
        md.append(String.format("| ERROR | %d | %d |\n", totalTdsError, totalRelError));
        md.append(String.format("| SKIP | %d | %d |\n", totalTdsSkip, totalRelSkip));
        md.append("\n---\n\n");

        // Error category summary
        Map<String, int[]> errorCategoryCounts = new LinkedHashMap<>();
        for (Map<String, FeatureCoverage> fm : categories.values())
        {
            for (FeatureCoverage fc : fm.values())
            {
                for (TestEntry te : fc.tests)
                {
                    FailureInfo tdsFi = failureMap.get(te.testId + "|TDS");
                    if (tdsFi != null)
                    {
                        errorCategoryCounts.computeIfAbsent(tdsFi.category, k -> new int[2])[0]++;
                    }
                    FailureInfo relFi = failureMap.get(te.testId + "|Relation");
                    if (relFi != null)
                    {
                        errorCategoryCounts.computeIfAbsent(relFi.category, k -> new int[2])[1]++;
                    }
                }
            }
        }

        if (!errorCategoryCounts.isEmpty())
        {
            md.append("## Error Categories\n\n");
            md.append("| Category | Description | TDS | Relation |\n");
            md.append("|----------|-------------|-----|----------|\n");
            for (Map.Entry<String, int[]> entry : errorCategoryCounts.entrySet())
            {
                md.append(String.format("| %s | %s | %d | %d |\n",
                        entry.getKey(),
                        ErrorCategorizer.description(entry.getKey()),
                        entry.getValue()[0], entry.getValue()[1]));
            }
            md.append("\n---\n\n");
        }

        // Category summary
        md.append("## Category Summary\n\n");
        md.append("| Category | Features | Tests | TDS PASS | TDS PARTIAL | TDS FAIL | TDS ERROR | TDS UNTESTED | Rel PASS | Rel PARTIAL | Rel FAIL | Rel ERROR | Rel UNTESTED |\n");
        md.append("|----------|----------|-------|----------|-------------|----------|-----------|--------------|----------|-------------|----------|-----------|-------------|\n");

        for (Map.Entry<String, Map<String, FeatureCoverage>> entry : categories.entrySet())
        {
            Map<String, FeatureCoverage> fm = entry.getValue();
            int tests = 0;
            for (FeatureCoverage fc : fm.values())
            {
                tests += fc.total;
            }
            int[] ct = countFeatureStatuses(fm.values(), true);
            int[] cr = countFeatureStatuses(fm.values(), false);
            md.append(String.format("| %s | %d | %d | %d | %d | %d | %d | %d | %d | %d | %d | %d | %d |\n",
                    entry.getKey(), fm.size(), tests,
                    ct[0], ct[1], ct[2], ct[3], ct[4],
                    cr[0], cr[1], cr[2], cr[3], cr[4]));
        }
        md.append("\n---\n\n");

        // Per-category feature tables with colour coding
        List<ErrorDetailEntry> allErrors = new ArrayList<>();

        for (Map.Entry<String, Map<String, FeatureCoverage>> entry : categories.entrySet())
        {
            md.append("## ").append(entry.getKey()).append("\n\n");
            md.append("| | Feature | Tests | TDS | Relation | Error Category |\n");
            md.append("|--|---------|-------|-----|----------|----------------|\n");

            for (FeatureCoverage fc : entry.getValue().values())
            {
                String tdsLabel = statusLabel(fc.tdsStatus(), fc.tdsPass, fc.total);
                String relLabel = statusLabel(fc.relStatus(), fc.relPass, fc.total);
                String rowEmoji = rowColour(fc.tdsStatus(), fc.relStatus());
                String errCat = getFeatureErrorCategory(fc, failureMap);
                String errLink = getFeatureErrorLink(fc, failureMap, errCat);

                md.append(String.format("| %s | %s | %d | %s | %s | %s |\n",
                        rowEmoji, fc.featureName, fc.total, tdsLabel, relLabel, errLink));

                // Collect errors for appendix
                for (TestEntry te : fc.tests)
                {
                    FailureInfo tdsFi = failureMap.get(te.testId + "|TDS");
                    if (tdsFi != null)
                    {
                        allErrors.add(new ErrorDetailEntry(te.testId, "TDS", tdsFi.sql, tdsFi.error, tdsFi.category));
                    }
                    FailureInfo relFi = failureMap.get(te.testId + "|Relation");
                    if (relFi != null)
                    {
                        allErrors.add(new ErrorDetailEntry(te.testId, "Relation", relFi.sql, relFi.error, relFi.category));
                    }
                }
            }
            md.append("\n");
        }

        // Error Detail Appendix
        if (!allErrors.isEmpty())
        {
            md.append("---\n\n");
            md.append("## Error Details\n\n");
            md.append("| Anchor | Test ID | Path | SQL | Error Category | Error |\n");
            md.append("|--------|---------|------|-----|----------------|-------|\n");
            for (ErrorDetailEntry e : allErrors)
            {
                String anchor = "fail-" + e.testId + "-" + e.path;
                String sqlSnippet = e.sql != null && e.sql.length() > 60
                        ? e.sql.substring(0, 60) + "..." : (e.sql != null ? e.sql : "");
                sqlSnippet = sqlSnippet.replace("|", "\\|");
                String errorSnippet = e.error != null && e.error.length() > 80
                        ? e.error.substring(0, 80) + "..." : (e.error != null ? e.error : "");
                errorSnippet = errorSnippet.replace("|", "\\|");
                md.append(String.format("| <a id=\"%s\"></a> | %s | %s | `%s` | %s | %s |\n",
                        anchor, e.testId, e.path, sqlSnippet, e.category, errorSnippet));
            }
            md.append("\n");
        }

        try (FileWriter fw = new FileWriter(path))
        {
            fw.write(md.toString());
        }
    }

    private static String rowColour(String tdsStatus, String relStatus)
    {
        boolean bothPass = "PASS".equals(tdsStatus) && "PASS".equals(relStatus);
        boolean bothError = "ERROR".equals(tdsStatus) && "ERROR".equals(relStatus);
        if (bothPass)
        {
            return "\uD83D\uDFE2"; // 🟢
        }
        if (bothError)
        {
            return "\uD83D\uDD34"; // 🔴
        }
        if ("PASS".equals(tdsStatus) || "PASS".equals(relStatus)
                || "PARTIAL".equals(tdsStatus) || "PARTIAL".equals(relStatus))
        {
            return "\uD83D\uDFE1"; // 🟡
        }
        if ("ERROR".equals(tdsStatus) || "ERROR".equals(relStatus)
                || "FAIL".equals(tdsStatus) || "FAIL".equals(relStatus))
        {
            return "\uD83D\uDD34"; // 🔴
        }
        return "⚪";
    }

    private static String statusLabel(String status, int pass, int total)
    {
        switch (status)
        {
            case "PASS":
                return String.format("PASS (%d/%d)", pass, total);
            case "PARTIAL":
                return String.format("PARTIAL (%d/%d)", pass, total);
            case "FAIL":
                return String.format("FAIL (%d/%d)", pass, total);
            case "ERROR":
                return String.format("ERROR (0/%d)", total);
            default:
                return "UNTESTED";
        }
    }

    private static String getFeatureErrorCategory(FeatureCoverage fc, Map<String, FailureInfo> failureMap)
    {
        for (TestEntry te : fc.tests)
        {
            FailureInfo fi = failureMap.get(te.testId + "|TDS");
            if (fi != null)
            {
                return fi.category;
            }
            fi = failureMap.get(te.testId + "|Relation");
            if (fi != null)
            {
                return fi.category;
            }
        }
        return "";
    }

    private static String getFeatureErrorLink(FeatureCoverage fc, Map<String, FailureInfo> failureMap, String category)
    {
        if (category.isEmpty())
        {
            return "";
        }
        boolean isFail = "FAIL".equals(fc.tdsStatus()) || "FAIL".equals(fc.relStatus());
        for (TestEntry te : fc.tests)
        {
            if (failureMap.containsKey(te.testId + "|TDS"))
            {
                String anchor = "fail-" + te.testId + "-TDS";
                return isFail
                        ? String.format("[%s](failure-details.md#%s)", category, anchor)
                        : String.format("[%s](#%s)", category, anchor);
            }
            if (failureMap.containsKey(te.testId + "|Relation"))
            {
                String anchor = "fail-" + te.testId + "-Relation";
                return isFail
                        ? String.format("[%s](failure-details.md#%s)", category, anchor)
                        : String.format("[%s](#%s)", category, anchor);
            }
        }
        return category;
    }

    private static class FailureInfo
    {
        final String id;
        final String path;
        final String state;
        final String sql;
        final String error;
        final String category;

        FailureInfo(String id, String path, String state, String sql, String error, String category)
        {
            this.id = id;
            this.path = path;
            this.state = state;
            this.sql = sql;
            this.error = error;
            this.category = category;
        }
    }

    private static class ErrorDetailEntry
    {
        final String testId;
        final String path;
        final String sql;
        final String error;
        final String category;

        ErrorDetailEntry(String testId, String path, String sql, String error, String category)
        {
            this.testId = testId;
            this.path = path;
            this.sql = sql;
            this.error = error;
            this.category = category;
        }
    }

    public void printConsoleSummary(Map<String, Map<String, FeatureCoverage>> categories)
    {
        List<FeatureCoverage> allFeatures = new ArrayList<>();
        int totalTests = 0;
        int totalTdsPass = 0;
        int totalTdsFail = 0;
        int totalTdsError = 0;
        int totalRelPass = 0;
        int totalRelFail = 0;
        int totalRelError = 0;
        for (Map<String, FeatureCoverage> fm : categories.values())
        {
            allFeatures.addAll(fm.values());
            for (FeatureCoverage fc : fm.values())
            {
                totalTests += fc.total;
                totalTdsPass += fc.tdsPass;
                totalTdsFail += fc.tdsFail;
                totalTdsError += fc.tdsError;
                totalRelPass += fc.relPass;
                totalRelFail += fc.relFail;
                totalRelError += fc.relError;
            }
        }

        int[] tds = countFeatureStatuses(allFeatures, true);
        int[] rel = countFeatureStatuses(allFeatures, false);

        LOGGER.info("");
        LOGGER.info("═══════════════════════════════════════════════════════════════════════════");
        LOGGER.info("SQL STRUCTURAL PARITY — Legend SQL (LegendSql)");
        LOGGER.info("═══════════════════════════════════════════════════════════════════════════");
        LOGGER.info(String.format("  Total features: %d  (%d tests)", allFeatures.size(), totalTests));
        LOGGER.info(String.format("  Total tests ran (TDS): %d  (pass: %d, fail: %d, error: %d)",
                totalTdsPass + totalTdsFail + totalTdsError, totalTdsPass, totalTdsFail, totalTdsError));
        LOGGER.info(String.format("  Total tests ran (Rel): %d  (pass: %d, fail: %d, error: %d)",
                totalRelPass + totalRelFail + totalRelError, totalRelPass, totalRelFail, totalRelError));
        LOGGER.info("");
        LOGGER.info("  TDS Path:");
        LOGGER.info(String.format("    ✅ PASS:      %d", tds[0]));
        LOGGER.info(String.format("    ⚠️  PARTIAL:   %d", tds[1]));
        LOGGER.info(String.format("    ❌ FAIL:      %d  (result mismatch)", tds[2]));
        LOGGER.info(String.format("    💥 ERROR:     %d  (server exception)", tds[3]));
        LOGGER.info(String.format("    ❓ UNTESTED:  %d", tds[4]));
        LOGGER.info("");
        LOGGER.info("  Relation Path:");
        LOGGER.info(String.format("    ✅ PASS:      %d", rel[0]));
        LOGGER.info(String.format("    ⚠️  PARTIAL:   %d", rel[1]));
        LOGGER.info(String.format("    ❌ FAIL:      %d  (result mismatch)", rel[2]));
        LOGGER.info(String.format("    💥 ERROR:     %d  (server exception)", rel[3]));
        LOGGER.info(String.format("    ❓ UNTESTED:  %d", rel[4]));
        LOGGER.info("═══════════════════════════════════════════════════════════════════════════");
        LOGGER.info("");
    }

}

