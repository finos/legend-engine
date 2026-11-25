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
import org.finos.legend.engine.postgres.e2e.ParityPath;
import org.finos.legend.engine.postgres.e2e.TestCaseLoader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
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
     * Per-test result for every parity path.
     */
    public static class TestEntry
    {
        public final String testId;
        public String tdsState;
        public String relationState;
        public String grammarState;

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
        public int grammarPass;
        public int grammarFail;
        public int grammarError;
        public int grammarSkip;
        public int grammarUnsupported;
        public final List<TestEntry> tests = new ArrayList<>();

        public FeatureCoverage(String featureName, String categoryName)
        {
            this.featureName = featureName;
            this.categoryName = categoryName;
        }

        public String tdsStatus()
        {
            return status(tdsPass, tdsFail, tdsSkip, tdsUnsupported);
        }

        public String relStatus()
        {
            return status(relPass, relFail, relSkip, relUnsupported);
        }

        public String grammarStatus()
        {
            return status(grammarPass, grammarFail, grammarSkip, grammarUnsupported);
        }

        public String status(ParityPath path)
        {
            switch (path)
            {
                case TDS:
                    return tdsStatus();
                case RELATION:
                    return relStatus();
                default:
                    return grammarStatus();
            }
        }

        public int pass(ParityPath path)
        {
            return counts(path)[0];
        }

        public int fail(ParityPath path)
        {
            return counts(path)[1];
        }

        public int error(ParityPath path)
        {
            return counts(path)[2];
        }

        public int skip(ParityPath path)
        {
            return counts(path)[3];
        }

        public int unsupported(ParityPath path)
        {
            return counts(path)[4];
        }

        private int[] counts(ParityPath path)
        {
            switch (path)
            {
                case TDS:
                    return new int[] {tdsPass, tdsFail, tdsError, tdsSkip, tdsUnsupported};
                case RELATION:
                    return new int[] {relPass, relFail, relError, relSkip, relUnsupported};
                default:
                    return new int[] {grammarPass, grammarFail, grammarError, grammarSkip, grammarUnsupported};
            }
        }

        private String status(int pass, int fail, int skip, int unsupported)
        {
            int effective = total - skip - unsupported;
            if (effective == 0)
            {
                return unsupported > 0 ? "UNSUPPORTED" : "UNTESTED";
            }
            if (pass == effective)
            {
                return "PASS";
            }
            if (pass > 0)
            {
                return "PARTIAL";
            }
            if (fail > 0)
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
        Map<String, Map<String, FeatureCoverage>> categories = buildCategories(allTestCases, reportFile);
        if (categories.isEmpty())
        {
            return; // no structural tests
        }

        Map<String, CoverageReportSupport.FailureInfo> failureMap = CoverageReportSupport.loadFailures(reportFile);
        new File(outputDir).mkdirs();
        generateJson(categories, outputDir + "/structural-parity.json");
        generateMarkdown(categories, outputDir + "/structural-parity.md", failureMap);
        printConsoleSummary(categories);
    }

    /**
     * Builds the category -&gt; feature -&gt; coverage model without writing any output, so
     * other consumers (e.g. the parity site generator) can reuse the same aggregation
     * logic instead of re-deriving it from the raw test cases and report file.
     */
    public Map<String, Map<String, FeatureCoverage>> buildCategories(List<TestCaseLoader.TestCase> allTestCases, File reportFile) throws IOException
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
                    entry.grammarState = "SKIP";
                    fc.tdsSkip++;
                    fc.relSkip++;
                    fc.grammarSkip++;
                }
            }
        }

        if (categories.isEmpty())
        {
            return categories; // no structural tests
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
                        else if ("Grammar".equals(path) && entry.grammarState == null)
                        {
                            entry.grammarState = resolvedState;
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
                fc.grammarPass = fc.grammarFail = fc.grammarError = fc.grammarSkip = fc.grammarUnsupported = 0;
                for (TestEntry e : fc.tests)
                {
                    tallyState(e.tdsState, fc, ParityPath.TDS);
                    tallyState(e.relationState, fc, ParityPath.RELATION);
                    tallyState(e.grammarState, fc, ParityPath.GRAMMAR);
                }
            }
        }

        return categories;
    }

    private void tallyState(String state, FeatureCoverage fc, ParityPath path)
    {
        int pass = 0;
        int fail = 0;
        int error = 0;
        int skip = 0;
        int unsupported = 0;
        if (state == null)
        {
            skip = 1;
        }
        else if ("PASS".equals(state))
        {
            pass = 1;
        }
        else if ("FAIL".equals(state))
        {
            fail = 1;
        }
        else if ("ERROR".equals(state) || "BUG".equals(state))
        {
            error = 1;
        }
        else if (state.startsWith("UNSUPPORTED"))
        {
            unsupported = 1;
        }
        else
        {
            skip = 1;
        }
        switch (path)
        {
            case TDS:
                fc.tdsPass += pass;
                fc.tdsFail += fail;
                fc.tdsError += error;
                fc.tdsSkip += skip;
                fc.tdsUnsupported += unsupported;
                break;
            case RELATION:
                fc.relPass += pass;
                fc.relFail += fail;
                fc.relError += error;
                fc.relSkip += skip;
                fc.relUnsupported += unsupported;
                break;
            default:
                fc.grammarPass += pass;
                fc.grammarFail += fail;
                fc.grammarError += error;
                fc.grammarSkip += skip;
                fc.grammarUnsupported += unsupported;
                break;
        }
    }

    private static int totalTestsOf(List<FeatureCoverage> features)
    {
        return features.stream().mapToInt(fc -> fc.total).sum();
    }

    private static void appendPathRow(StringBuilder md, String label, java.util.function.Function<ParityPath, String> cell)
    {
        md.append("| ").append(label);
        for (ParityPath path : ParityPath.values())
        {
            md.append(" | ").append(cell.apply(path));
        }
        md.append(" |\n");
    }

    private static int[] countFeatureStatuses(Iterable<FeatureCoverage> features, ParityPath path)
    {
        // [pass, partial, fail, error, untested, unsupported]
        int[] c = new int[6];
        for (FeatureCoverage fc : features)
        {
            String s = fc.status(path);
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

        ObjectNode summary = root.putObject("summary");
        summary.put("total_features", allFeatures.size());
        summary.put("total_tests", totalTests);
        for (ParityPath parityPath : ParityPath.values())
        {
            putCounts(summary.putObject(parityPath.reportKey()), countFeatureStatuses(allFeatures, parityPath));
        }

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

                for (ParityPath parityPath : ParityPath.values())
                {
                    ObjectNode pathNode = fnNode.putObject(parityPath.reportKey());
                    pathNode.put("status", fc.status(parityPath));
                    pathNode.put("pass", fc.pass(parityPath));
                    pathNode.put("fail", fc.fail(parityPath));
                    pathNode.put("error", fc.error(parityPath));
                    pathNode.put("total", fc.total);
                }

                ArrayNode details = fnNode.putArray("testDetails");
                for (TestEntry te : fc.tests)
                {
                    ObjectNode d = details.addObject();
                    d.put("id", te.testId);
                    d.put("tds", te.tdsState != null ? te.tdsState : "UNKNOWN");
                    d.put("relation", te.relationState != null ? te.relationState : "UNKNOWN");
                    d.put("grammar", te.grammarState != null ? te.grammarState : "UNKNOWN");
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

    private void generateMarkdown(Map<String, Map<String, FeatureCoverage>> categories, String path, Map<String, CoverageReportSupport.FailureInfo> failureMap) throws IOException
    {
        StringBuilder md = new StringBuilder();
        md.append("# SQL Structural Parity — Legend SQL (LegendSql)\n\n");

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

        // Percentages exclude unsupported and skip from denominator
        Map<ParityPath, Integer> passByPath = new EnumMap<>(ParityPath.class);
        Map<ParityPath, Integer> failByPath = new EnumMap<>(ParityPath.class);
        Map<ParityPath, Integer> errorByPath = new EnumMap<>(ParityPath.class);
        Map<ParityPath, Integer> skipByPath = new EnumMap<>(ParityPath.class);
        Map<ParityPath, Integer> unsupportedByPath = new EnumMap<>(ParityPath.class);
        Map<ParityPath, Double> passPctByPath = new EnumMap<>(ParityPath.class);
        for (ParityPath parityPath : ParityPath.values())
        {
            int pass = 0;
            int fail = 0;
            int error = 0;
            int skip = 0;
            int unsupported = 0;
            for (FeatureCoverage fc : allFeatures)
            {
                pass += fc.pass(parityPath);
                fail += fc.fail(parityPath);
                error += fc.error(parityPath);
                skip += fc.skip(parityPath);
                unsupported += fc.unsupported(parityPath);
            }
            int effective = totalTests - skip - unsupported;
            passByPath.put(parityPath, pass);
            failByPath.put(parityPath, fail);
            errorByPath.put(parityPath, error);
            skipByPath.put(parityPath, skip);
            unsupportedByPath.put(parityPath, unsupported);
            passPctByPath.put(parityPath, effective > 0 ? (100.0 * pass / effective) : 0);
        }

        md.append("## Summary\n\n");
        md.append("| Metric | TDS | Relation | Grammar |\n|--------|-----|----------|---------|\n");
        appendPathRow(md, "Total features", parityPath -> String.valueOf(allFeatures.size()));
        appendPathRow(md, "Total tests", parityPath -> String.valueOf(totalTestsOf(allFeatures)));
        appendPathRow(md, "⚪ UNSUPPORTED", parityPath -> String.valueOf(unsupportedByPath.get(parityPath)));
        appendPathRow(md, "✅ PASS", parityPath -> String.format("%d (%.1f%%)", passByPath.get(parityPath), passPctByPath.get(parityPath)));
        appendPathRow(md, "❌ FAIL", parityPath -> String.valueOf(failByPath.get(parityPath)));
        appendPathRow(md, "💥 ERROR", parityPath -> String.valueOf(errorByPath.get(parityPath)));
        appendPathRow(md, "❓ SKIP", parityPath -> String.valueOf(skipByPath.get(parityPath)));
        appendPathRow(md, "**Pass rate**", parityPath -> String.format("**%.1f%%**", passPctByPath.get(parityPath)));
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
                    for (ParityPath parityPath : ParityPath.values())
                    {
                        CoverageReportSupport.FailureInfo fi = failureMap.get(CoverageReportSupport.failureKey(te.testId, parityPath));
                        if (fi != null)
                        {
                            errorCategoryCounts.computeIfAbsent(fi.category, k -> new int[ParityPath.values().length])[parityPath.ordinal()]++;
                        }
                    }
                }
            }
        }

        if (!errorCategoryCounts.isEmpty())
        {
            md.append("## Error Categories\n\n");
            md.append("| Category | Description | TDS | Relation | Grammar |\n");
            md.append("|----------|-------------|-----|----------|---------|\n");
            for (Map.Entry<String, int[]> entry : errorCategoryCounts.entrySet())
            {
                String catAnchor = entry.getKey().toLowerCase().replace("_", "-");
                md.append(String.format("| [%s](#%s) | %s | %d | %d | %d |\n",
                        entry.getKey(),
                        catAnchor,
                        ErrorCategorizer.description(entry.getKey()),
                        entry.getValue()[0], entry.getValue()[1], entry.getValue()[2]));
            }
            md.append("\n---\n\n");
        }

        // Category summary
        md.append("## Category Summary\n\n");
        md.append("| Category | Features | Tests | TDS PASS | TDS PARTIAL | TDS FAIL | TDS ERROR | TDS UNTESTED | Rel PASS | Rel PARTIAL | Rel FAIL | Rel ERROR | Rel UNTESTED | Grammar PASS | Grammar PARTIAL | Grammar FAIL | Grammar ERROR | Grammar UNTESTED |\n");
        md.append("|----------|----------|-------|----------|-------------|----------|-----------|--------------|----------|-------------|----------|-----------|-------------|--------------|-----------------|--------------|---------------|------------------|\n");

        for (Map.Entry<String, Map<String, FeatureCoverage>> entry : categories.entrySet())
        {
            Map<String, FeatureCoverage> fm = entry.getValue();
            int tests = 0;
            for (FeatureCoverage fc : fm.values())
            {
                tests += fc.total;
            }
            int[] ct = countFeatureStatuses(fm.values(), ParityPath.TDS);
            int[] cr = countFeatureStatuses(fm.values(), ParityPath.RELATION);
            int[] cg = countFeatureStatuses(fm.values(), ParityPath.GRAMMAR);
            String catAnchor = entry.getKey().toLowerCase().replace(" ", "-");
            md.append(String.format("| [%s](#%s) | %d | %d | %d | %d | %d | %d | %d | %d | %d | %d | %d | %d | %d | %d | %d | %d | %d |\n",
                    entry.getKey(), catAnchor, fm.size(), tests,
                    ct[0], ct[1], ct[2], ct[3], ct[4],
                    cr[0], cr[1], cr[2], cr[3], cr[4],
                    cg[0], cg[1], cg[2], cg[3], cg[4]));
        }
        md.append("\n---\n\n");

        // Per-category feature tables with colour coding
        List<CoverageReportSupport.ErrorDetailEntry> allErrors = new ArrayList<>();

        for (Map.Entry<String, Map<String, FeatureCoverage>> entry : categories.entrySet())
        {
            String catHeadingAnchor = entry.getKey().toLowerCase().replace(" ", "-");
            md.append("<a id=\"").append(catHeadingAnchor).append("\"></a>\n\n");
            md.append("## ").append(entry.getKey()).append("\n\n");
            md.append("| | Feature | Tests | TDS | Relation | Grammar | Error Category |\n");
            md.append("|--|---------|-------|-----|----------|---------|----------------|\n");

            for (FeatureCoverage fc : entry.getValue().values())
            {
                String tdsLabel = statusLabel(fc.tdsStatus(), fc.tdsPass, fc.total);
                String relLabel = statusLabel(fc.relStatus(), fc.relPass, fc.total);
                String grammarLabel = statusLabel(fc.grammarStatus(), fc.grammarPass, fc.total);
                String rowEmoji = rowColour(fc.tdsStatus(), fc.relStatus(), fc.grammarStatus());
                String errCat = getFeatureErrorCategory(fc, failureMap);
                String errLink = getFeatureErrorLink(fc, failureMap, errCat);

                md.append(String.format("| %s | %s | %d | %s | %s | %s | %s |\n",
                        rowEmoji, fc.featureName, fc.total, tdsLabel, relLabel, grammarLabel, errLink));

                // Collect errors for appendix
                for (TestEntry te : fc.tests)
                {
                    for (ParityPath parityPath : ParityPath.values())
                    {
                        CoverageReportSupport.FailureInfo fi = failureMap.get(CoverageReportSupport.failureKey(te.testId, parityPath));
                        if (fi != null)
                        {
                            allErrors.add(new CoverageReportSupport.ErrorDetailEntry(te.testId, parityPath.label(), fi.sql, fi.rewrittenSql, fi.error, fi.category));
                        }
                    }
                }
            }
            md.append("\n");
        }

        if (!allErrors.isEmpty())
        {
            md.append("---\n\n");
            CoverageReportSupport.appendErrorDetailsSection(md, allErrors);
        }

        try (FileWriter fw = new FileWriter(path))
        {
            fw.write(md.toString());
        }
    }

    private static String rowColour(String tdsStatus, String relStatus, String grammarStatus)
    {
        List<String> statuses = new ArrayList<>();
        statuses.add(tdsStatus);
        statuses.add(relStatus);
        if (CoverageReportSupport.isRecorded(grammarStatus))
        {
            statuses.add(grammarStatus);
        }
        boolean allPass = statuses.stream().allMatch("PASS"::equals);
        boolean allError = statuses.stream().allMatch("ERROR"::equals);
        if (allPass)
        {
            return "\uD83D\uDFE2"; // 🟢
        }
        if (allError)
        {
            return "\uD83D\uDD34"; // 🔴
        }
        if (statuses.stream().anyMatch(st -> "PASS".equals(st) || "PARTIAL".equals(st)))
        {
            return "\uD83D\uDFE1"; // 🟡
        }
        if (statuses.stream().anyMatch(st -> "ERROR".equals(st) || "FAIL".equals(st)))
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

    private static String getFeatureErrorCategory(FeatureCoverage fc, Map<String, CoverageReportSupport.FailureInfo> failureMap)
    {
        return String.join(", ", featureCategories(fc, failureMap));
    }

    private static java.util.Set<String> featureCategories(FeatureCoverage fc, Map<String, CoverageReportSupport.FailureInfo> failureMap)
    {
        java.util.Set<String> categories = new java.util.LinkedHashSet<>();
        for (TestEntry te : fc.tests)
        {
            for (ParityPath path : ParityPath.values())
            {
                CoverageReportSupport.FailureInfo fi = failureMap.get(CoverageReportSupport.failureKey(te.testId, path));
                if (fi != null)
                {
                    categories.add(fi.category);
                }
            }
        }
        return categories;
    }

    private static String getFeatureErrorLink(FeatureCoverage fc, Map<String, CoverageReportSupport.FailureInfo> failureMap, String category)
    {
        if (category.isEmpty())
        {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String cat : featureCategories(fc, failureMap))
        {
            if (!first)
            {
                sb.append(", ");
            }
            first = false;
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

    private static String findFirstAnchorForCategory(FeatureCoverage fc, Map<String, CoverageReportSupport.FailureInfo> failureMap, String category)
    {
        for (TestEntry te : fc.tests)
        {
            for (ParityPath path : ParityPath.values())
            {
                CoverageReportSupport.FailureInfo fi = failureMap.get(CoverageReportSupport.failureKey(te.testId, path));
                if (fi != null && category.equals(fi.category))
                {
                    return "fail-" + te.testId + "-" + path.label();
                }
            }
        }
        return null;
    }

    public void printConsoleSummary(Map<String, Map<String, FeatureCoverage>> categories)
    {
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

        LOGGER.info("");
        LOGGER.info("═══════════════════════════════════════════════════════════════════════════");
        LOGGER.info("SQL STRUCTURAL PARITY — Legend SQL (LegendSql)");
        LOGGER.info("═══════════════════════════════════════════════════════════════════════════");
        LOGGER.info(String.format("  Total features: %d  (%d tests)", allFeatures.size(), totalTests));
        for (ParityPath path : ParityPath.values())
        {
            int pass = allFeatures.stream().mapToInt(fc -> fc.pass(path)).sum();
            int fail = allFeatures.stream().mapToInt(fc -> fc.fail(path)).sum();
            int error = allFeatures.stream().mapToInt(fc -> fc.error(path)).sum();
            int unsupported = allFeatures.stream().mapToInt(fc -> fc.unsupported(path)).sum();
            LOGGER.info(String.format("  Total tests ran (%s): %d  (pass: %d, fail: %d, error: %d, unsupported: %d)",
                    path == ParityPath.RELATION ? "Rel" : path.label(), pass + fail + error, pass, fail, error, unsupported));
        }
        for (ParityPath path : ParityPath.values())
        {
            int[] counts = countFeatureStatuses(allFeatures, path);
            int pass = allFeatures.stream().mapToInt(fc -> fc.pass(path)).sum();
            int unsupported = allFeatures.stream().mapToInt(fc -> fc.unsupported(path)).sum();
            int effective = totalTests - unsupported;
            double passPct = effective > 0 ? (100.0 * pass / effective) : 0;
            LOGGER.info("");
            LOGGER.info("  " + path.label() + " Path:");
            LOGGER.info(String.format("    ✅ PASS:        %d (%.1f%%)", counts[0], passPct));
            LOGGER.info(String.format("    ⚠️  PARTIAL:     %d", counts[1]));
            LOGGER.info(String.format("    ❌ FAIL:        %d  (result mismatch)", counts[2]));
            LOGGER.info(String.format("    💥 ERROR:       %d  (server exception)", counts[3]));
            LOGGER.info(String.format("    ❓ UNTESTED:    %d", counts[4]));
            LOGGER.info(String.format("    ⚪ UNSUPPORTED: %d", unsupported));
        }
        LOGGER.info("═══════════════════════════════════════════════════════════════════════════");
        LOGGER.info("");
    }

}

