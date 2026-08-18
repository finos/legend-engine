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
        public int tdsUnsupported;
        public int relPass;
        public int relFail;
        public int relError;
        public int relSkip;
        public int relUnsupported;
        public final List<TestEntry> tests = new ArrayList<>();

        public FeatureCoverage(String featureName, String categoryName)
        {
            this.featureName = featureName;
            this.categoryName = categoryName;
        }

        public String tdsStatus()
        {
            int effective = total - tdsSkip - tdsUnsupported;
            if (effective == 0)
            {
                return tdsUnsupported > 0 ? "UNSUPPORTED" : "UNTESTED";
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
            int effective = total - relSkip - relUnsupported;
            if (effective == 0)
            {
                return relUnsupported > 0 ? "UNSUPPORTED" : "UNTESTED";
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

            // Build error lookup from failures array (id|path -> error), same approach as FunctionCoverageMapper
            Map<String, String> errorLookup = new HashMap<>();
            JsonNode failures = report.get("failures");
            if (failures != null)
            {
                for (JsonNode failure : failures)
                {
                    String fId = failure.has("id") ? failure.get("id").asText() : "";
                    String fPath = failure.has("path") ? failure.get("path").asText() : "";
                    String fError = failure.has("error") ? failure.get("error").asText() : "";
                    errorLookup.put(fId + "|" + fPath, fError);
                }
            }

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
                        // Reclassify ERROR/BUG as UNSUPPORTED using error from failures array
                        String resolvedState = state;
                        if ("ERROR".equals(state) || "BUG".equals(state))
                        {
                            String error = errorLookup.getOrDefault(id + "|" + path, "");
                            String category = ErrorCategorizer.categorize(state, error);
                            if (ErrorCategorizer.UNSUPPORTED_SYNTAX.equals(category)
                                    || ErrorCategorizer.UNSUPPORTED.equals(category)
                                    || ErrorCategorizer.FUNCTION_NOT_SUPPORTED.equals(category)
                                    || ErrorCategorizer.FUNCTION_NO_SQL_TRANSLATION.equals(category))
                            {
                                resolvedState = "UNSUPPORTED";
                            }
                        }
                        if ("TDS".equals(path) && entry.tdsState == null)
                        {
                            entry.tdsState = resolvedState;
                        }
                        else if ("Relation".equals(path) && entry.relationState == null)
                        {
                            entry.relationState = resolvedState;
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
                fc.tdsPass = fc.tdsFail = fc.tdsError = fc.tdsSkip = fc.tdsUnsupported = 0;
                fc.relPass = fc.relFail = fc.relError = fc.relSkip = fc.relUnsupported = 0;
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
                    String rewrittenSql = f.has("rewrittenSql") ? f.get("rewrittenSql").asText() : "";
                    String category = ErrorCategorizer.categorize(state, error);
                    map.put(id + "|" + pathVal, new FailureInfo(id, pathVal, state, sql, rewrittenSql, error, category));
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
            if ("PASS".equals(state))
            {
                fc.tdsPass++;
            }
            else if ("FAIL".equals(state))
            {
                fc.tdsFail++;
            }
            else if ("ERROR".equals(state) || "BUG".equals(state))
            {
                fc.tdsError++;
            }
            else if (state.startsWith("UNSUPPORTED"))
            {
                fc.tdsUnsupported++;
            }
            else
            {
                fc.tdsSkip++;
            }
        }
        else
        {
            if ("PASS".equals(state))
            {
                fc.relPass++;
            }
            else if ("FAIL".equals(state))
            {
                fc.relFail++;
            }
            else if ("ERROR".equals(state) || "BUG".equals(state))
            {
                fc.relError++;
            }
            else if (state.startsWith("UNSUPPORTED"))
            {
                fc.relUnsupported++;
            }
            else
            {
                fc.relSkip++;
            }
        }
    }

    private static int[] countFeatureStatuses(Iterable<FeatureCoverage> features, boolean tds)
    {
        // [pass, partial, fail, error, untested, unsupported]
        int[] c = new int[6];
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
                case "UNSUPPORTED":
                    c[5]++;
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
        node.put("unsupported", c[5]);
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
        int totalTdsUnsupported = 0;
        int totalRelPass = 0;
        int totalRelFail = 0;
        int totalRelError = 0;
        int totalRelSkip = 0;
        int totalRelUnsupported = 0;
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
                totalTdsUnsupported += fc.tdsUnsupported;
                totalRelPass += fc.relPass;
                totalRelFail += fc.relFail;
                totalRelError += fc.relError;
                totalRelSkip += fc.relSkip;
                totalRelUnsupported += fc.relUnsupported;
            }
        }

        // Percentages exclude unsupported and skip from denominator
        int tdsEffective = totalTests - totalTdsSkip - totalTdsUnsupported;
        int relEffective = totalTests - totalRelSkip - totalRelUnsupported;
        double tdsPct = tdsEffective > 0 ? (100.0 * totalTdsPass / tdsEffective) : 0;
        double relPct = relEffective > 0 ? (100.0 * totalRelPass / relEffective) : 0;

        md.append("## Summary\n\n");
        md.append("| Metric | TDS | Relation |\n|--------|-----|----------|\n");
        md.append(String.format("| Total features | %d | %d |\n", allFeatures.size(), allFeatures.size()));
        md.append(String.format("| Total tests | %d | %d |\n", totalTests, totalTests));
        md.append(String.format("| ⚪ UNSUPPORTED | %d | %d |\n", totalTdsUnsupported, totalRelUnsupported));
        md.append(String.format("| ✅ PASS | %d (%.1f%%) | %d (%.1f%%) |\n", totalTdsPass, tdsPct, totalRelPass, relPct));
        md.append(String.format("| ❌ FAIL | %d | %d |\n", totalTdsFail, totalRelFail));
        md.append(String.format("| 💥 ERROR | %d | %d |\n", totalTdsError, totalRelError));
        md.append(String.format("| ❓ SKIP | %d | %d |\n", totalTdsSkip, totalRelSkip));
        md.append(String.format("| **Pass rate** | **%.1f%%** | **%.1f%%** |\n", tdsPct, relPct));
        md.append("\n_Percentages exclude UNSUPPORTED and SKIP from the denominator._\n");
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
                String catAnchor = entry.getKey().toLowerCase().replace("_", "-");
                md.append(String.format("| [%s](#%s) | %s | %d | %d |\n",
                        entry.getKey(),
                        catAnchor,
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
            String catAnchor = entry.getKey().toLowerCase().replace(" ", "-");
            md.append(String.format("| [%s](#%s) | %d | %d | %d | %d | %d | %d | %d | %d | %d | %d | %d | %d |\n",
                    entry.getKey(), catAnchor, fm.size(), tests,
                    ct[0], ct[1], ct[2], ct[3], ct[4],
                    cr[0], cr[1], cr[2], cr[3], cr[4]));
        }
        md.append("\n---\n\n");

        // Per-category feature tables with colour coding
        List<ErrorDetailEntry> allErrors = new ArrayList<>();

        for (Map.Entry<String, Map<String, FeatureCoverage>> entry : categories.entrySet())
        {
            String catHeadingAnchor = entry.getKey().toLowerCase().replace(" ", "-");
            md.append("<a id=\"").append(catHeadingAnchor).append("\"></a>\n\n");
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
                        allErrors.add(new ErrorDetailEntry(te.testId, "TDS", tdsFi.sql, tdsFi.rewrittenSql, tdsFi.error, tdsFi.category));
                    }
                    FailureInfo relFi = failureMap.get(te.testId + "|Relation");
                    if (relFi != null)
                    {
                        allErrors.add(new ErrorDetailEntry(te.testId, "Relation", relFi.sql, relFi.rewrittenSql, relFi.error, relFi.category));
                    }
                }
            }
            md.append("\n");
        }

        // Error Detail Appendix — organized by category, grouped by test ID
        if (!allErrors.isEmpty())
        {
            md.append("---\n\n");
            md.append("## Error Details\n\n");

            // Group by category, then by test ID
            Map<String, Map<String, List<ErrorDetailEntry>>> byCategoryThenTest = new LinkedHashMap<>();
            for (ErrorDetailEntry e : allErrors)
            {
                byCategoryThenTest
                        .computeIfAbsent(e.category, k -> new LinkedHashMap<>())
                        .computeIfAbsent(e.testId, k -> new ArrayList<>())
                        .add(e);
            }

            for (Map.Entry<String, Map<String, List<ErrorDetailEntry>>> catEntry : byCategoryThenTest.entrySet())
            {
                String category = catEntry.getKey();
                String catAnchor = category.toLowerCase().replace("_", "-");
                md.append("<a id=\"").append(catAnchor).append("\"></a>\n\n");
                md.append("### ").append(category);
                md.append(" (").append(catEntry.getValue().size()).append(" tests)\n\n");

                boolean firstTest = true;
                for (Map.Entry<String, List<ErrorDetailEntry>> testEntry : catEntry.getValue().entrySet())
                {
                    String testId = testEntry.getKey();
                    List<ErrorDetailEntry> entries = testEntry.getValue();

                    if (!firstTest)
                    {
                        md.append("\n<br>\n\n");
                    }
                    firstTest = false;

                    // Create anchors for all paths
                    StringBuilder anchors = new StringBuilder();
                    for (ErrorDetailEntry e : entries)
                    {
                        String anchor = "fail-" + e.testId + "-" + e.path;
                        anchors.append(String.format("<a id=\"%s\"></a>", anchor));
                    }

                    md.append("#### ").append(anchors).append("`").append(testId).append("`\n\n");

                    // Check if TDS and Relation have same error and category — if so, merge
                    if (entries.size() == 2)
                    {
                        ErrorDetailEntry e1 = entries.get(0);
                        ErrorDetailEntry e2 = entries.get(1);
                        boolean sameError = (e1.error == null ? "" : e1.error).equals(e2.error == null ? "" : e2.error);
                        boolean sameCategory = e1.category.equals(e2.category);

                        if (sameError && sameCategory)
                        {
                            md.append("\uD83D\uDD34 **Failed in both TDS and Relation**\n\n");
                            md.append("**Input SQL:**\n```sql\n").append(e1.sql != null ? e1.sql : "").append("\n```\n\n");
                            String legendSql1 = e1.rewrittenSql != null && !e1.rewrittenSql.isEmpty() ? e1.rewrittenSql : "";
                            String legendSql2 = e2.rewrittenSql != null && !e2.rewrittenSql.isEmpty() ? e2.rewrittenSql : "";
                            if (!legendSql1.isEmpty() || !legendSql2.isEmpty())
                            {
                                if (legendSql1.equals(legendSql2) || legendSql2.isEmpty())
                                {
                                    md.append("**Legend SQL:**\n```sql\n").append(legendSql1).append("\n```\n\n");
                                }
                                else if (legendSql1.isEmpty())
                                {
                                    md.append("**Legend SQL:**\n```sql\n").append(legendSql2).append("\n```\n\n");
                                }
                                else
                                {
                                    md.append("**Legend SQL (TDS):**\n```sql\n").append(legendSql1).append("\n```\n\n");
                                    md.append("**Legend SQL (Relation):**\n```sql\n").append(legendSql2).append("\n```\n\n");
                                }
                            }
                            md.append("**Error:**\n> ").append(e1.error != null ? e1.error.replace("\n", "\n> ") : "").append("\n\n");
                            continue;
                        }
                    }

                    // List each path separately
                    for (ErrorDetailEntry e : entries)
                    {
                        String pathEmoji = "TDS".equals(e.path) ? "\uD83D\uDCD8" : "\uD83D\uDCD7";
                        md.append(String.format("%s **%s Path**\n\n", pathEmoji, e.path));
                        md.append("**Input SQL:**\n```sql\n").append(e.sql != null ? e.sql : "").append("\n```\n\n");
                        if (e.rewrittenSql != null && !e.rewrittenSql.isEmpty())
                        {
                            md.append("**Legend SQL:**\n```sql\n").append(e.rewrittenSql).append("\n```\n\n");
                        }
                        md.append("**Error:**\n> ").append(e.error != null ? e.error.replace("\n", "\n> ") : "").append("\n\n");
                    }
                }
                md.append("\n");
            }
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
        // Collect all distinct error categories for this feature
        java.util.Set<String> categories = new java.util.LinkedHashSet<>();
        for (TestEntry te : fc.tests)
        {
            FailureInfo fi = failureMap.get(te.testId + "|TDS");
            if (fi != null)
            {
                categories.add(fi.category);
            }
            fi = failureMap.get(te.testId + "|Relation");
            if (fi != null)
            {
                categories.add(fi.category);
            }
        }
        if (categories.isEmpty())
        {
            return "";
        }
        return String.join(", ", categories);
    }

    private static String getFeatureErrorLink(FeatureCoverage fc, Map<String, FailureInfo> failureMap, String category)
    {
        if (category.isEmpty())
        {
            return "";
        }
        // Collect all distinct categories and link each to its Error Details heading
        java.util.Set<String> categories = new java.util.LinkedHashSet<>();
        for (TestEntry te : fc.tests)
        {
            FailureInfo fi = failureMap.get(te.testId + "|TDS");
            if (fi != null)
            {
                categories.add(fi.category);
            }
            fi = failureMap.get(te.testId + "|Relation");
            if (fi != null)
            {
                categories.add(fi.category);
            }
        }

        // Build linked list: each category links to its heading, plus link to first error for this feature
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String cat : categories)
        {
            if (!first)
            {
                sb.append(", ");
            }
            first = false;
            // Find the first test in this feature that has this category for an anchor
            String anchor = findFirstAnchorForCategory(fc, failureMap, cat);
            if (anchor != null)
            {
                sb.append(String.format("[%s](#%s)", cat, anchor));
            }
            else
            {
                String catAnchor = cat.toLowerCase().replace("_", "-");
                sb.append(String.format("[%s](#%s)", cat, catAnchor));
            }
        }
        return sb.toString();
    }

    private static String findFirstAnchorForCategory(FeatureCoverage fc, Map<String, FailureInfo> failureMap, String category)
    {
        for (TestEntry te : fc.tests)
        {
            FailureInfo fi = failureMap.get(te.testId + "|TDS");
            if (fi != null && category.equals(fi.category))
            {
                return "fail-" + te.testId + "-TDS";
            }
            fi = failureMap.get(te.testId + "|Relation");
            if (fi != null && category.equals(fi.category))
            {
                return "fail-" + te.testId + "-Relation";
            }
        }
        return null;
    }

    private static class FailureInfo
    {
        final String id;
        final String path;
        final String state;
        final String sql;
        final String rewrittenSql;
        final String error;
        final String category;

        FailureInfo(String id, String path, String state, String sql, String rewrittenSql, String error, String category)
        {
            this.id = id;
            this.path = path;
            this.state = state;
            this.sql = sql;
            this.rewrittenSql = rewrittenSql;
            this.error = error;
            this.category = category;
        }
    }

    private static class ErrorDetailEntry
    {
        final String testId;
        final String path;
        final String sql;
        final String rewrittenSql;
        final String error;
        final String category;

        ErrorDetailEntry(String testId, String path, String sql, String rewrittenSql, String error, String category)
        {
            this.testId = testId;
            this.path = path;
            this.sql = sql;
            this.rewrittenSql = rewrittenSql;
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
        int totalTdsUnsupported = 0;
        int totalRelPass = 0;
        int totalRelFail = 0;
        int totalRelError = 0;
        int totalRelUnsupported = 0;
        for (Map<String, FeatureCoverage> fm : categories.values())
        {
            allFeatures.addAll(fm.values());
            for (FeatureCoverage fc : fm.values())
            {
                totalTests += fc.total;
                totalTdsPass += fc.tdsPass;
                totalTdsFail += fc.tdsFail;
                totalTdsError += fc.tdsError;
                totalTdsUnsupported += fc.tdsUnsupported;
                totalRelPass += fc.relPass;
                totalRelFail += fc.relFail;
                totalRelError += fc.relError;
                totalRelUnsupported += fc.relUnsupported;
            }
        }

        int[] tds = countFeatureStatuses(allFeatures, true);
        int[] rel = countFeatureStatuses(allFeatures, false);

        int tdsEffective = totalTests - totalTdsUnsupported;
        int relEffective = totalTests - totalRelUnsupported;
        double tdsPct = tdsEffective > 0 ? (100.0 * totalTdsPass / tdsEffective) : 0;
        double relPct = relEffective > 0 ? (100.0 * totalRelPass / relEffective) : 0;

        LOGGER.info("");
        LOGGER.info("═══════════════════════════════════════════════════════════════════════════");
        LOGGER.info("SQL STRUCTURAL PARITY — Legend SQL (LegendSql)");
        LOGGER.info("═══════════════════════════════════════════════════════════════════════════");
        LOGGER.info(String.format("  Total features: %d  (%d tests)", allFeatures.size(), totalTests));
        LOGGER.info(String.format("  Total tests ran (TDS): %d  (pass: %d, fail: %d, error: %d, unsupported: %d)",
                totalTdsPass + totalTdsFail + totalTdsError, totalTdsPass, totalTdsFail, totalTdsError, totalTdsUnsupported));
        LOGGER.info(String.format("  Total tests ran (Rel): %d  (pass: %d, fail: %d, error: %d, unsupported: %d)",
                totalRelPass + totalRelFail + totalRelError, totalRelPass, totalRelFail, totalRelError, totalRelUnsupported));
        LOGGER.info("");
        LOGGER.info("  TDS Path:");
        LOGGER.info(String.format("    ✅ PASS:        %d (%.1f%%)", tds[0], tdsPct));
        LOGGER.info(String.format("    ⚠️  PARTIAL:     %d", tds[1]));
        LOGGER.info(String.format("    ❌ FAIL:        %d  (result mismatch)", tds[2]));
        LOGGER.info(String.format("    💥 ERROR:       %d  (server exception)", tds[3]));
        LOGGER.info(String.format("    ❓ UNTESTED:    %d", tds[4]));
        LOGGER.info(String.format("    ⚪ UNSUPPORTED: %d", totalTdsUnsupported));
        LOGGER.info("");
        LOGGER.info("  Relation Path:");
        LOGGER.info(String.format("    ✅ PASS:        %d (%.1f%%)", rel[0], relPct));
        LOGGER.info(String.format("    ⚠️  PARTIAL:     %d", rel[1]));
        LOGGER.info(String.format("    ❌ FAIL:        %d  (result mismatch)", rel[2]));
        LOGGER.info(String.format("    💥 ERROR:       %d  (server exception)", rel[3]));
        LOGGER.info(String.format("    ❓ UNTESTED:    %d", rel[4]));
        LOGGER.info(String.format("    ⚪ UNSUPPORTED: %d", totalRelUnsupported));
        LOGGER.info("═══════════════════════════════════════════════════════════════════════════");
        LOGGER.info("");
    }

}

