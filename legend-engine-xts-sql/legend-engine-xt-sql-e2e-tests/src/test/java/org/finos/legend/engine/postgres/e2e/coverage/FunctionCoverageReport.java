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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates function coverage reports in JSON and Markdown format.
 * Reports both TDS and Relation path status independently per signature.
 * Distinguishes between ERROR (server exception) and FAIL (result mismatch).
 */
public class FunctionCoverageReport
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FunctionCoverageReport.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Generates both JSON and Markdown reports.
     */
    public void generate(Map<String, List<FunctionCatalogExtractor.PgFunction>> catalog, String outputDir) throws IOException
    {
        generate(catalog, outputDir, null);
    }

    /**
     * Generates both JSON and Markdown reports with error detail appendix.
     *
     * @param catalog    function catalog grouped by category
     * @param outputDir  output directory for reports
     * @param parityReportFile  parity-report.json file for error details (may be null)
     */
    public void generate(Map<String, List<FunctionCatalogExtractor.PgFunction>> catalog, String outputDir, File parityReportFile) throws IOException
    {
        new File(outputDir).mkdirs();
        generateJson(catalog, outputDir + "/function-coverage.json");
        generateMarkdown(catalog, outputDir + "/function-coverage.md", parityReportFile);
        printConsoleSummary(catalog);
    }

    private static int[] countStatuses(Iterable<FunctionCatalogExtractor.PgFunction> fns, boolean tds)
    {
        // [pass, partial, fail, error, untested]
        int[] c = new int[5];
        for (FunctionCatalogExtractor.PgFunction fn : fns)
        {
            String s = tds ? fn.tdsStatus : fn.relStatus;
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
                case "NOT_APPLICABLE":
                    break;
                default:
                    c[4]++;
                    break;
            }
        }
        return c;
    }

    private void generateJson(Map<String, List<FunctionCatalogExtractor.PgFunction>> catalog, String path) throws IOException
    {
        ObjectNode root = MAPPER.createObjectNode();

        // Summary counts
        int total = 0;
        int[] tds = new int[5];
        int[] rel = new int[5];
        for (List<FunctionCatalogExtractor.PgFunction> fns : catalog.values())
        {
            total += fns.size();
            int[] t = countStatuses(fns, true);
            int[] r = countStatuses(fns, false);
            for (int i = 0; i < 5; i++)
            {
                tds[i] += t[i];
                rel[i] += r[i];
            }
        }

        ObjectNode summary = root.putObject("summary");
        summary.put("total", total);
        putPathSummary(summary.putObject("tds"), tds);
        putPathSummary(summary.putObject("relation"), rel);

        // Categories
        ArrayNode categories = root.putArray("categories");
        for (Map.Entry<String, List<FunctionCatalogExtractor.PgFunction>> entry : catalog.entrySet())
        {
            if (entry.getValue().isEmpty())
            {
                continue;
            }
            ObjectNode catNode = categories.addObject();
            catNode.put("name", entry.getKey());

            ArrayNode functions = catNode.putArray("functions");
            for (FunctionCatalogExtractor.PgFunction fn : entry.getValue())
            {
                ObjectNode fnNode = functions.addObject();
                fnNode.put("name", fn.name);
                fnNode.put("signature", fn.signature);

                // TDS status
                ObjectNode tdsNode = fnNode.putObject("tds");
                tdsNode.put("status", fn.tdsStatus);
                if (fn.coverage != null)
                {
                    tdsNode.put("pass", fn.coverage.tdsPass);
                    tdsNode.put("fail", fn.coverage.tdsFail);
                    tdsNode.put("error", fn.coverage.tdsError);
                    tdsNode.put("total", fn.coverage.total);
                }

                // Relation status
                ObjectNode relNode = fnNode.putObject("relation");
                relNode.put("status", fn.relStatus);
                if (fn.coverage != null)
                {
                    relNode.put("pass", fn.coverage.relPass);
                    relNode.put("fail", fn.coverage.relFail);
                    relNode.put("error", fn.coverage.relError);
                    relNode.put("total", fn.coverage.total);
                }

                // Test details
                if (fn.coverage != null && !fn.coverage.testDetails.isEmpty())
                {
                    ArrayNode details = fnNode.putArray("testDetails");
                    for (FunctionCoverageMapper.TestResultEntry te : fn.coverage.testDetails)
                    {
                        ObjectNode d = details.addObject();
                        d.put("id", te.testId);
                        d.put("tds", te.tdsState != null ? te.tdsState : "UNKNOWN");
                        d.put("relation", te.relationState != null ? te.relationState : "UNKNOWN");
                    }
                }

                if (!fn.notes.isEmpty())
                {
                    fnNode.put("notes", fn.notes);
                }
            }
        }

        MAPPER.writeValue(new File(path), root);
    }

    private static void putPathSummary(ObjectNode node, int[] counts)
    {
        node.put("pass", counts[0]);
        node.put("partial", counts[1]);
        node.put("fail", counts[2]);
        node.put("error", counts[3]);
        node.put("untested", counts[4]);
    }

    private void generateMarkdown(Map<String, List<FunctionCatalogExtractor.PgFunction>> catalog, String path, File parityReportFile) throws IOException
    {
        // Load error details from parity report
        Map<String, FailureInfo> failureMap = loadFailures(parityReportFile);

        StringBuilder md = new StringBuilder();
        md.append("# Postgres Function Coverage — Legend SQL (LegendSql)\n\n");

        // === 1. Summary ===
        int total = 0;
        int[] tds = new int[5];
        int[] rel = new int[5];
        for (List<FunctionCatalogExtractor.PgFunction> fns : catalog.values())
        {
            total += fns.size();
            int[] t = countStatuses(fns, true);
            int[] r = countStatuses(fns, false);
            for (int i = 0; i < 5; i++)
            {
                tds[i] += t[i];
                rel[i] += r[i];
            }
        }

        // Count total tests ran from coverage details
        int totalTdsPass = 0;
        int totalTdsFail = 0;
        int totalTdsError = 0;
        int totalTdsSkip = 0;
        int totalRelPass = 0;
        int totalRelFail = 0;
        int totalRelError = 0;
        int totalRelSkip = 0;
        int totalTests = 0;
        Set<String> uniqueNames = new HashSet<>();
        for (List<FunctionCatalogExtractor.PgFunction> fns : catalog.values())
        {
            for (FunctionCatalogExtractor.PgFunction fn : fns)
            {
                uniqueNames.add(fn.name);
                if (fn.coverage != null)
                {
                    totalTests += fn.coverage.total;
                    totalTdsPass += fn.coverage.tdsPass;
                    totalTdsFail += fn.coverage.tdsFail;
                    totalTdsError += fn.coverage.tdsError;
                    totalTdsSkip += fn.coverage.tdsSkip;
                    totalRelPass += fn.coverage.relPass;
                    totalRelFail += fn.coverage.relFail;
                    totalRelError += fn.coverage.relError;
                    totalRelSkip += fn.coverage.relSkip;
                }
            }
        }

        md.append("## Summary\n\n");
        md.append("| Metric | TDS | Relation |\n|--------|-----|----------|\n");
        md.append(String.format("| Total signatures | %d | %d |\n", total, total));
        md.append(String.format("| Unique function names | %d | %d |\n", uniqueNames.size(), uniqueNames.size()));
        md.append(String.format("| Total tests | %d | %d |\n", totalTests, totalTests));
        md.append(String.format("| PASS | %d | %d |\n", totalTdsPass, totalRelPass));
        md.append(String.format("| FAIL | %d | %d |\n", totalTdsFail, totalRelFail));
        md.append(String.format("| ERROR | %d | %d |\n", totalTdsError, totalRelError));
        md.append(String.format("| SKIP | %d | %d |\n", totalTdsSkip, totalRelSkip));
        md.append("\n---\n\n");

        // === 2. Error Categories ===
        Map<String, int[]> errorCategoryCounts = new LinkedHashMap<>();
        for (List<FunctionCatalogExtractor.PgFunction> fns : catalog.values())
        {
            for (FunctionCatalogExtractor.PgFunction fn : fns)
            {
                categorizeFunction(fn, failureMap, errorCategoryCounts);
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

        // === 3. Category Summary ===
        md.append("## Category Summary\n\n");
        md.append("| Category | Total | TDS PASS | TDS PARTIAL | TDS FAIL | TDS ERROR | TDS UNTESTED | Rel PASS | Rel PARTIAL | Rel FAIL | Rel ERROR | Rel UNTESTED |\n");
        md.append("|----------|-------|----------|-------------|----------|-----------|--------------|----------|-------------|----------|-----------|-------------|\n");

        for (Map.Entry<String, List<FunctionCatalogExtractor.PgFunction>> entry : catalog.entrySet())
        {
            if (entry.getValue().isEmpty())
            {
                continue;
            }
            int ct = entry.getValue().size();
            int[] ct_ = countStatuses(entry.getValue(), true);
            int[] cr_ = countStatuses(entry.getValue(), false);
            md.append(String.format("| %s | %d | %d | %d | %d | %d | %d | %d | %d | %d | %d | %d |\n",
                    entry.getKey(), ct,
                    ct_[0], ct_[1], ct_[2], ct_[3], ct_[4],
                    cr_[0], cr_[1], cr_[2], cr_[3], cr_[4]));
        }
        md.append("\n---\n\n");

        // === 4. Per-category function results ===
        List<ErrorDetailEntry> allErrors = new ArrayList<>();

        for (Map.Entry<String, List<FunctionCatalogExtractor.PgFunction>> entry : catalog.entrySet())
        {
            if (entry.getValue().isEmpty())
            {
                continue;
            }
            md.append("## ").append(entry.getKey()).append("\n\n");
            md.append("| | Function | Signature | TDS | Relation | Error Category | Notes |\n");
            md.append("|--|----------|-----------|-----|----------|----------------|-------|\n");

            for (FunctionCatalogExtractor.PgFunction fn : entry.getValue())
            {
                String tdsLabel = statusToLabel(fn.tdsStatus, fn.coverage, true);
                String relLabel = statusToLabel(fn.relStatus, fn.coverage, false);
                String rowEmoji = rowColour(fn.tdsStatus, fn.relStatus);
                String sig = fn.signature.replace("|", "\\|");
                String errCat = getErrorCategory(fn, failureMap);
                String errLink = getErrorLink(fn, failureMap);

                md.append(String.format("| %s | `%s` | `%s` | %s | %s | %s | %s |\n",
                        rowEmoji, fn.name, sig, tdsLabel, relLabel,
                        errCat.isEmpty() ? "" : errLink,
                        fn.notes));

                collectErrors(fn, failureMap, allErrors);
            }
            md.append("\n");
        }

        md.append("---\n\n");

        // === 5. Functions Not Implemented ===
        Map<String, List<String>> notImplementedByCategory = new LinkedHashMap<>();
        for (Map.Entry<String, List<FunctionCatalogExtractor.PgFunction>> entry : catalog.entrySet())
        {
            for (FunctionCatalogExtractor.PgFunction fn : entry.getValue())
            {
                if ("ERROR".equals(fn.tdsStatus))
                {
                    String errCat = getErrorCategory(fn, failureMap);
                    if (ErrorCategorizer.FUNCTION_NOT_SUPPORTED.equals(errCat)
                            || ErrorCategorizer.FUNCTION_NO_SQL_TRANSLATION.equals(errCat))
                    {
                        notImplementedByCategory
                                .computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                                .add(fn.name);
                    }
                }
            }
        }

        if (!notImplementedByCategory.isEmpty())
        {
            int totalNotImpl = notImplementedByCategory.values().stream().mapToInt(List::size).sum();
            md.append("## Functions Not Implemented (").append(totalNotImpl).append(")\n\n");
            md.append("These Postgres functions are recognized in the catalog but Legend SQL does not implement them.\n");
            md.append("They fail with \"No function matches the given name\" or \"No SQL translation exists\".\n\n");

            for (Map.Entry<String, List<String>> entry : notImplementedByCategory.entrySet())
            {
                List<String> names = entry.getValue();
                List<String> unique = new ArrayList<>();
                for (String name : names)
                {
                    if (!unique.contains(name))
                    {
                        unique.add(name);
                    }
                }
                md.append("### ").append(entry.getKey())
                        .append(" (").append(unique.size()).append(")\n\n");
                md.append("| Function |\n");
                md.append("|----------|\n");
                for (String name : unique)
                {
                    md.append("| `").append(name).append("` |\n");
                }
                md.append("\n");
            }
            md.append("---\n\n");
        }

        // === 6. Error Details ===
        if (!allErrors.isEmpty())
        {
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
            md.append("\n---\n\n");
        }

        // === 7. Unsupported Functions ===
        Map<String, Boolean> functionHasSupport = new LinkedHashMap<>();
        for (List<FunctionCatalogExtractor.PgFunction> fns : catalog.values())
        {
            for (FunctionCatalogExtractor.PgFunction fn : fns)
            {
                if (!"NOT_APPLICABLE".equals(fn.tdsStatus))
                {
                    functionHasSupport.put(fn.name, Boolean.TRUE);
                }
                else
                {
                    functionHasSupport.putIfAbsent(fn.name, Boolean.FALSE);
                }
            }
        }

        boolean hasFullyUnsupported = functionHasSupport.containsValue(Boolean.FALSE);

        if (hasFullyUnsupported)
        {
            md.append("## Unsupported Functions\n\n");
            md.append("Functions listed below have no supported signatures in Legend SQL.\n");
            md.append("Functions with at least one working overload (e.g., `length(text)`) are excluded.\n\n");

            for (Map.Entry<String, List<FunctionCatalogExtractor.PgFunction>> entry : catalog.entrySet())
            {
                List<FunctionCatalogExtractor.PgFunction> fullyUnsupported = new ArrayList<>();
                for (FunctionCatalogExtractor.PgFunction fn : entry.getValue())
                {
                    if ("NOT_APPLICABLE".equals(fn.tdsStatus)
                            && Boolean.FALSE.equals(functionHasSupport.get(fn.name)))
                    {
                        fullyUnsupported.add(fn);
                    }
                }
                if (fullyUnsupported.isEmpty())
                {
                    continue;
                }
                Map<String, FunctionCatalogExtractor.PgFunction> byName = new LinkedHashMap<>();
                for (FunctionCatalogExtractor.PgFunction fn : fullyUnsupported)
                {
                    byName.putIfAbsent(fn.name, fn);
                }

                md.append("### ").append(entry.getKey())
                        .append(" (").append(byName.size()).append(" unsupported)\n\n");
                md.append("| Function | Reason |\n");
                md.append("|----------|--------|\n");
                for (FunctionCatalogExtractor.PgFunction fn : byName.values())
                {
                    String reason = fn.notes.isEmpty() ? "Unsupported category/type" : fn.notes;
                    md.append(String.format("| `%s` | %s |\n", fn.name, reason));
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
        boolean bothError = ("ERROR".equals(tdsStatus) || "NOT_APPLICABLE".equals(tdsStatus))
                && ("ERROR".equals(relStatus) || "NOT_APPLICABLE".equals(relStatus));
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
        return "⚪"; // untested/unknown
    }

    private static String statusToLabel(String status, FunctionCoverageMapper.SignatureCoverage cov, boolean isTds)
    {
        int pass = cov != null ? (isTds ? cov.tdsPass : cov.relPass) : 0;
        int tot = cov != null ? cov.total : 0;
        switch (status)
        {
            case "PASS":
                return String.format("PASS (%d/%d)", pass, tot);
            case "PARTIAL":
                return String.format("PARTIAL (%d/%d)", pass, tot);
            case "FAIL":
                return String.format("FAIL (%d/%d)", pass, tot);
            case "ERROR":
                return String.format("ERROR (0/%d)", tot);
            case "NOT_APPLICABLE":
                return "UNSUPPORTED";
            default:
                return "UNTESTED";
        }
    }

    private static String getErrorCategory(FunctionCatalogExtractor.PgFunction fn, Map<String, FailureInfo> failureMap)
    {
        if ("PASS".equals(fn.tdsStatus) && "PASS".equals(fn.relStatus))
        {
            return "";
        }
        if ("UNTESTED".equals(fn.tdsStatus) && "UNTESTED".equals(fn.relStatus))
        {
            return "";
        }
        if (fn.coverage != null && !fn.coverage.testDetails.isEmpty())
        {
            for (FunctionCoverageMapper.TestResultEntry te : fn.coverage.testDetails)
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
        }
        // Infer from status
        if ("ERROR".equals(fn.tdsStatus) || "ERROR".equals(fn.relStatus))
        {
            return ErrorCategorizer.MISC;
        }
        if ("FAIL".equals(fn.tdsStatus) || "FAIL".equals(fn.relStatus))
        {
            return ErrorCategorizer.RESULT_MISMATCH;
        }
        return "";
    }

    private static String getErrorLink(FunctionCatalogExtractor.PgFunction fn, Map<String, FailureInfo> failureMap)
    {
        String cat = getErrorCategory(fn, failureMap);
        if (cat.isEmpty())
        {
            return "";
        }
        if (fn.coverage != null && !fn.coverage.testDetails.isEmpty())
        {
            FunctionCoverageMapper.TestResultEntry te = fn.coverage.testDetails.get(0);
            String anchor = "fail-" + te.testId + "-TDS";
            return String.format("[%s](failure-details.md#%s)", cat, anchor);
        }
        return cat;
    }

    private static void collectErrors(FunctionCatalogExtractor.PgFunction fn, Map<String, FailureInfo> failureMap, List<ErrorDetailEntry> allErrors)
    {
        if (fn.coverage == null)
        {
            return;
        }
        for (FunctionCoverageMapper.TestResultEntry te : fn.coverage.testDetails)
        {
            FailureInfo tdsFailure = failureMap.get(te.testId + "|TDS");
            if (tdsFailure != null)
            {
                allErrors.add(new ErrorDetailEntry(te.testId, "TDS", tdsFailure.sql, tdsFailure.error, tdsFailure.category));
            }
            FailureInfo relFailure = failureMap.get(te.testId + "|Relation");
            if (relFailure != null)
            {
                allErrors.add(new ErrorDetailEntry(te.testId, "Relation", relFailure.sql, relFailure.error, relFailure.category));
            }
        }
    }

    private static void categorizeFunction(FunctionCatalogExtractor.PgFunction fn, Map<String, FailureInfo> failureMap, Map<String, int[]> counts)
    {
        String tdsCategory = null;
        String relCategory = null;

        if (fn.coverage != null)
        {
            for (FunctionCoverageMapper.TestResultEntry te : fn.coverage.testDetails)
            {
                if (tdsCategory == null)
                {
                    FailureInfo fi = failureMap.get(te.testId + "|TDS");
                    if (fi != null)
                    {
                        tdsCategory = fi.category;
                    }
                }
                if (relCategory == null)
                {
                    FailureInfo fi = failureMap.get(te.testId + "|Relation");
                    if (fi != null)
                    {
                        relCategory = fi.category;
                    }
                }
            }
        }

        if (tdsCategory != null)
        {
            counts.computeIfAbsent(tdsCategory, k -> new int[2])[0]++;
        }
        if (relCategory != null)
        {
            counts.computeIfAbsent(relCategory, k -> new int[2])[1]++;
        }
    }

    private static Map<String, FailureInfo> loadFailures(File parityReportFile)
    {
        Map<String, FailureInfo> map = new LinkedHashMap<>();
        if (parityReportFile == null || !parityReportFile.exists())
        {
            return map;
        }
        try
        {
            JsonNode report = MAPPER.readTree(parityReportFile);
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

    public void printConsoleSummary(Map<String, List<FunctionCatalogExtractor.PgFunction>> catalog)
    {
        int total = 0;
        int[] tds = new int[5];
        int[] rel = new int[5];
        for (List<FunctionCatalogExtractor.PgFunction> fns : catalog.values())
        {
            total += fns.size();
            int[] t = countStatuses(fns, true);
            int[] r = countStatuses(fns, false);
            for (int i = 0; i < 5; i++)
            {
                tds[i] += t[i];
                rel[i] += r[i];
            }
        }

        // Count total tests
        int totalTdsPass = 0;
        int totalTdsFail = 0;
        int totalTdsError = 0;
        int totalRelPass = 0;
        int totalRelFail = 0;
        int totalRelError = 0;
        Set<String> uniqueNames = new HashSet<>();
        for (List<FunctionCatalogExtractor.PgFunction> fns : catalog.values())
        {
            for (FunctionCatalogExtractor.PgFunction fn : fns)
            {
                uniqueNames.add(fn.name);
                if (fn.coverage != null)
                {
                    totalTdsPass += fn.coverage.tdsPass;
                    totalTdsFail += fn.coverage.tdsFail;
                    totalTdsError += fn.coverage.tdsError;
                    totalRelPass += fn.coverage.relPass;
                    totalRelFail += fn.coverage.relFail;
                    totalRelError += fn.coverage.relError;
                }
            }
        }

        LOGGER.info("");
        LOGGER.info("═══════════════════════════════════════════════════════════════════════════");
        LOGGER.info("POSTGRES FUNCTION COVERAGE — Legend SQL (LegendSql)");
        LOGGER.info("═══════════════════════════════════════════════════════════════════════════");
        LOGGER.info(String.format("  Total signatures: %d", total));
        LOGGER.info(String.format("  Unique function names: %d", uniqueNames.size()));
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
