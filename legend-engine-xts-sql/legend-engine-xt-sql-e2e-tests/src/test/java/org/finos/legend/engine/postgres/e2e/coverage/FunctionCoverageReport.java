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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates function coverage reports in JSON and Markdown format.
 * Reports TDS, Relation and Grammar path status independently per signature.
 * Distinguishes between ERROR (server exception) and FAIL (result mismatch).
 */
public class FunctionCoverageReport
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FunctionCoverageReport.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final Set<ParityPath> LEGACY_PATHS = EnumSet.of(ParityPath.TDS, ParityPath.RELATION);
    private static final Set<ParityPath> GRAMMAR_ONLY = EnumSet.of(ParityPath.GRAMMAR);

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

    private static int[] countStatuses(Iterable<FunctionCatalogExtractor.PgFunction> fns, ParityPath path)
    {
        // [pass, partial, fail, error, untested]
        int[] c = new int[5];
        for (FunctionCatalogExtractor.PgFunction fn : fns)
        {
            String s = statusOf(fn, path);
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

    private static void markUnsupported(FunctionCatalogExtractor.PgFunction fn)
    {
        fn.tdsStatus = "NOT_APPLICABLE";
        fn.relStatus = "NOT_APPLICABLE";
    }

    private static String statusOf(FunctionCatalogExtractor.PgFunction fn, ParityPath path)
    {
        switch (path)
        {
            case TDS:
                return fn.tdsStatus;
            case RELATION:
                return fn.relStatus;
            default:
                return fn.grammarStatus;
        }
    }

    private static int[] passFailError(FunctionCoverageMapper.SignatureCoverage coverage, ParityPath path)
    {
        switch (path)
        {
            case TDS:
                return new int[] {coverage.tdsPass, coverage.tdsFail, coverage.tdsError};
            case RELATION:
                return new int[] {coverage.relPass, coverage.relFail, coverage.relError};
            default:
                return new int[] {coverage.grammarPass, coverage.grammarFail, coverage.grammarError};
        }
    }

    private static String stateOf(FunctionCoverageMapper.TestResultEntry entry, ParityPath path)
    {
        switch (path)
        {
            case TDS:
                return entry.tdsState;
            case RELATION:
                return entry.relationState;
            default:
                return entry.grammarState;
        }
    }

    private static int supportedFunctionCount(Map<String, List<FunctionCatalogExtractor.PgFunction>> catalog, ParityPath path)
    {
        java.util.Map<String, String> bestStatusByName = new java.util.HashMap<>();
        for (List<FunctionCatalogExtractor.PgFunction> fns : catalog.values())
        {
            for (FunctionCatalogExtractor.PgFunction fn : fns)
            {
                String existing = bestStatusByName.get(fn.name);
                if (existing == null || isStatusBetter(statusOf(fn, path), existing))
                {
                    bestStatusByName.put(fn.name, statusOf(fn, path));
                }
            }
        }
        int supported = 0;
        for (String status : bestStatusByName.values())
        {
            if ("PASS".equals(status) || "PARTIAL".equals(status))
            {
                supported++;
            }
        }
        return supported;
    }

    private void generateJson(Map<String, List<FunctionCatalogExtractor.PgFunction>> catalog, String path) throws IOException
    {
        ObjectNode root = MAPPER.createObjectNode();

        // Summary counts
        int total = 0;
        int[] tds = new int[5];
        int[] rel = new int[5];
        int[] grammar = new int[5];
        int totalTests = 0;
        java.util.Map<String, String> functionNameStatus = new java.util.HashMap<>();
        for (List<FunctionCatalogExtractor.PgFunction> fns : catalog.values())
        {
            total += fns.size();
            int[] t = countStatuses(fns, ParityPath.TDS);
            int[] r = countStatuses(fns, ParityPath.RELATION);
            int[] g = countStatuses(fns, ParityPath.GRAMMAR);
            for (int i = 0; i < 5; i++)
            {
                tds[i] += t[i];
                rel[i] += r[i];
                grammar[i] += g[i];
            }

            // Track best status for each unique function name
            for (FunctionCatalogExtractor.PgFunction fn : fns)
            {
                if (fn.coverage != null)
                {
                    totalTests += fn.coverage.total;
                }

                String existingStatus = functionNameStatus.get(fn.name);
                String currentStatus = getBestStatus(fn.tdsStatus, fn.relStatus, fn.grammarStatus);

                if (existingStatus == null || isStatusBetter(currentStatus, existingStatus))
                {
                    functionNameStatus.put(fn.name, currentStatus);
                }
            }
        }

        int uniqueFunctionCount = functionNameStatus.size();
        
        int tdsSupported = supportedFunctionCount(catalog, ParityPath.TDS);
        int relSupported = supportedFunctionCount(catalog, ParityPath.RELATION);
        int grammarSupported = supportedFunctionCount(catalog, ParityPath.GRAMMAR);

        ObjectNode summary = root.putObject("summary");
        summary.put("total", total);
        summary.put("unique_function_names", uniqueFunctionCount);
        summary.put("tds_supported_functions", tdsSupported);
        summary.put("relation_supported_functions", relSupported);
        summary.put("grammar_supported_functions", grammarSupported);
        summary.put("total_tests", totalTests);
        putPathSummary(summary.putObject("tds"), tds, total);
        putPathSummary(summary.putObject("relation"), rel, total);
        putPathSummary(summary.putObject("grammar"), grammar, total);

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

                for (ParityPath parityPath : ParityPath.values())
                {
                    ObjectNode pathNode = fnNode.putObject(parityPath.reportKey());
                    pathNode.put("status", statusOf(fn, parityPath));
                    if (fn.coverage != null)
                    {
                        int[] passFailError = passFailError(fn.coverage, parityPath);
                        pathNode.put("pass", passFailError[0]);
                        pathNode.put("fail", passFailError[1]);
                        pathNode.put("error", passFailError[2]);
                        pathNode.put("total", fn.coverage.total);
                    }
                }

                // Test details
                if (fn.coverage != null && !fn.coverage.testDetails.isEmpty())
                {
                    ArrayNode details = fnNode.putArray("testDetails");
                    for (FunctionCoverageMapper.TestResultEntry te : fn.coverage.testDetails)
                    {
                        ObjectNode d = details.addObject();
                        d.put("id", te.testId);
                        for (ParityPath parityPath : ParityPath.values())
                        {
                            String state = stateOf(te, parityPath);
                            d.put(parityPath.reportKey(), state != null ? state : "UNKNOWN");
                        }
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

    /**
     * Get the best status across the TDS, Relation and Grammar paths.
     * PASS > PARTIAL > UNTESTED > FAIL > ERROR > NOT_APPLICABLE
     */
    private static String getBestStatus(String tdsStatus, String relStatus, String grammarStatus)
    {
        String best = tdsStatus;
        if (isStatusBetter(relStatus, best))
        {
            best = relStatus;
        }
        if (isStatusBetter(grammarStatus, best))
        {
            best = grammarStatus;
        }
        return best;
    }

    /**
     * Returns true if status1 is better than status2.
     */
    private static boolean isStatusBetter(String status1, String status2)
    {
        return statusRank(status1) < statusRank(status2);
    }

    private static int statusRank(String status)
    {
        switch (status)
        {
            case "PASS": return 0;
            case "PARTIAL": return 1;
            case "UNTESTED": return 2;
            case "FAIL": return 3;
            case "ERROR": return 4;
            case "NOT_APPLICABLE": return 5;
            default: return 6;
        }
    }

    private static void putPathSummary(ObjectNode node, int[] counts, int total)
    {
        node.put("pass", counts[0]);
        node.put("partial", counts[1]);
        node.put("fail", counts[2]);
        node.put("error", counts[3]);
        node.put("untested", counts[4]);
        int accounted = counts[0] + counts[1] + counts[2] + counts[3] + counts[4];
        node.put("unsupported", total - accounted);
    }

    private void generateMarkdown(Map<String, List<FunctionCatalogExtractor.PgFunction>> catalog, String path, File parityReportFile) throws IOException
    {
        // Load error details from parity report
        Map<String, CoverageReportSupport.FailureInfo> failureMap = CoverageReportSupport.loadFailures(parityReportFile);

        // Reclassify FUNCTION_NOT_SUPPORTED and UNSUPPORTED_SYNTAX as NOT_APPLICABLE (unsupported, not error)
        for (List<FunctionCatalogExtractor.PgFunction> fns : catalog.values())
        {
            for (FunctionCatalogExtractor.PgFunction fn : fns)
            {
                String errCat = getErrorCategory(fn, failureMap, LEGACY_PATHS);
                if (errCat.contains(ErrorCategorizer.FUNCTION_NOT_SUPPORTED))
                {
                    markUnsupported(fn);
                    if (fn.notes.isEmpty())
                    {
                        fn.notes = "Function not supported";
                    }
                }
                else if (errCat.contains(ErrorCategorizer.UNSUPPORTED_SYNTAX))
                {
                    markUnsupported(fn);
                    if (fn.notes.isEmpty())
                    {
                        fn.notes = "Unsupported feature";
                    }
                }

                String grammarCat = getErrorCategory(fn, failureMap, GRAMMAR_ONLY);
                if (grammarCat.contains(ErrorCategorizer.FUNCTION_NOT_SUPPORTED)
                        || grammarCat.contains(ErrorCategorizer.UNSUPPORTED_SYNTAX))
                {
                    fn.grammarStatus = "NOT_APPLICABLE";
                    if (fn.notes.isEmpty())
                    {
                        fn.notes = grammarCat.contains(ErrorCategorizer.FUNCTION_NOT_SUPPORTED)
                                ? "Grammar: function not supported"
                                : "Grammar: unsupported feature";
                    }
                }
            }
        }

        StringBuilder md = new StringBuilder();
        md.append("# Postgres Function Coverage — Legend SQL (LegendSql)\n\n");

        // === 1. Summary ===
        int total = 0;
        for (List<FunctionCatalogExtractor.PgFunction> fns : catalog.values())
        {
            total += fns.size();
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
        int totalGrammarPass = 0;
        int totalGrammarFail = 0;
        int totalGrammarError = 0;
        int totalGrammarSkip = 0;
        int totalTests = 0;
        int totalUnsupported = 0;
        int totalRelUnsupported = 0;
        int totalGrammarUnsupported = 0;
        Set<String> uniqueNames = new HashSet<>();
        java.util.Map<String, String> functionNameStatus = new java.util.HashMap<>();
        for (List<FunctionCatalogExtractor.PgFunction> fns : catalog.values())
        {
            for (FunctionCatalogExtractor.PgFunction fn : fns)
            {
                uniqueNames.add(fn.name);
                if ("NOT_APPLICABLE".equals(fn.tdsStatus))
                {
                    totalUnsupported++;
                }
                if ("NOT_APPLICABLE".equals(fn.relStatus))
                {
                    totalRelUnsupported++;
                }
                if ("NOT_APPLICABLE".equals(fn.grammarStatus))
                {
                    totalGrammarUnsupported++;
                }
                if (fn.coverage != null)
                {
                    totalTests += fn.coverage.total;
                    if (!"NOT_APPLICABLE".equals(fn.tdsStatus))
                    {
                        totalTdsPass += fn.coverage.tdsPass;
                        totalTdsFail += fn.coverage.tdsFail;
                        totalTdsError += fn.coverage.tdsError;
                        totalTdsSkip += fn.coverage.tdsSkip;
                    }
                    if (!"NOT_APPLICABLE".equals(fn.relStatus))
                    {
                        totalRelPass += fn.coverage.relPass;
                        totalRelFail += fn.coverage.relFail;
                        totalRelError += fn.coverage.relError;
                        totalRelSkip += fn.coverage.relSkip;
                    }
                    if (!"NOT_APPLICABLE".equals(fn.grammarStatus))
                    {
                        totalGrammarPass += fn.coverage.grammarPass;
                        totalGrammarFail += fn.coverage.grammarFail;
                        totalGrammarError += fn.coverage.grammarError;
                        totalGrammarSkip += fn.coverage.grammarSkip;
                    }
                }
                
                // Track best status for each unique function name
                String existingStatus = functionNameStatus.get(fn.name);
                String currentStatus = getBestStatus(fn.tdsStatus, fn.relStatus, fn.grammarStatus);
                if (existingStatus == null || isStatusBetter(currentStatus, existingStatus))
                {
                    functionNameStatus.put(fn.name, currentStatus);
                }
            }
        }
        
        int tdsSupported = supportedFunctionCount(catalog, ParityPath.TDS);
        int relSupported = supportedFunctionCount(catalog, ParityPath.RELATION);
        int grammarSupported = supportedFunctionCount(catalog, ParityPath.GRAMMAR);
        double tdsSupportedPct = uniqueNames.size() > 0 ? (100.0 * tdsSupported / uniqueNames.size()) : 0;
        double relSupportedPct = uniqueNames.size() > 0 ? (100.0 * relSupported / uniqueNames.size()) : 0;
        double grammarSupportedPct = uniqueNames.size() > 0 ? (100.0 * grammarSupported / uniqueNames.size()) : 0;

        md.append("## Summary\n\n");
        md.append("| Metric | TDS | Relation | Grammar |\n|--------|-----|----------|---------|\n");
        md.append(String.format("| Total signatures | %d | %d | %d |\n", total, total, total));
        md.append(String.format("| Unique function names | %d | %d | %d |\n", uniqueNames.size(), uniqueNames.size(), uniqueNames.size()));
        md.append(String.format("| **Supported functions (PASS/PARTIAL)** | **%d (%.1f%%)** | **%d (%.1f%%)** | **%d (%.1f%%)** |\n",
                tdsSupported, tdsSupportedPct, relSupported, relSupportedPct, grammarSupported, grammarSupportedPct));
        md.append(String.format("| Total tests | %d | %d | %d |\n", totalTests, totalTests, totalTests));
        md.append(String.format("| PASS | %d | %d | %d |\n", totalTdsPass, totalRelPass, totalGrammarPass));
        md.append(String.format("| FAIL | %d | %d | %d |\n", totalTdsFail, totalRelFail, totalGrammarFail));
        md.append(String.format("| ERROR | %d | %d | %d |\n", totalTdsError, totalRelError, totalGrammarError));
        md.append(String.format("| SKIP | %d | %d | %d |\n", totalTdsSkip, totalRelSkip, totalGrammarSkip));
        md.append(String.format("| UNSUPPORTED | %d | %d | %d |\n", totalUnsupported, totalRelUnsupported, totalGrammarUnsupported));
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

        CoverageReportSupport.appendErrorCategoriesSection(md, errorCategoryCounts);

        // === 3. Category Summary ===
        md.append("## Category Summary\n\n");
        md.append("| Category | Total | TDS PASS | TDS PARTIAL | TDS FAIL | TDS ERROR | TDS UNTESTED | Rel PASS | Rel PARTIAL | Rel FAIL | Rel ERROR | Rel UNTESTED | Grammar PASS | Grammar PARTIAL | Grammar FAIL | Grammar ERROR | Grammar UNTESTED |\n");
        md.append("|----------|-------|----------|-------------|----------|-----------|--------------|----------|-------------|----------|-----------|-------------|--------------|-----------------|--------------|---------------|------------------|\n");

        for (Map.Entry<String, List<FunctionCatalogExtractor.PgFunction>> entry : catalog.entrySet())
        {
            if (entry.getValue().isEmpty())
            {
                continue;
            }
            int ct = entry.getValue().size();
            int[] ct_ = countStatuses(entry.getValue(), ParityPath.TDS);
            int[] cr_ = countStatuses(entry.getValue(), ParityPath.RELATION);
            int[] cg_ = countStatuses(entry.getValue(), ParityPath.GRAMMAR);
            String catAnchor = entry.getKey().toLowerCase().replace(" ", "-");
            String docUrl = DocLinks.urlFor(entry.getKey());
            String label = docUrl != null ? String.format("[%s](%s) / [details](#%s)", entry.getKey(), docUrl, catAnchor)
                    : String.format("[%s](#%s)", entry.getKey(), catAnchor);
            md.append(String.format("| %s | %d | %d | %d | %d | %d | %d | %d | %d | %d | %d | %d | %d | %d | %d | %d | %d |\n",
                    label, ct,
                    ct_[0], ct_[1], ct_[2], ct_[3], ct_[4],
                    cr_[0], cr_[1], cr_[2], cr_[3], cr_[4],
                    cg_[0], cg_[1], cg_[2], cg_[3], cg_[4]));
        }
        md.append("\n---\n\n");

        // === 4. Per-category function results ===
        List<CoverageReportSupport.ErrorDetailEntry> allErrors = new ArrayList<>();

        for (Map.Entry<String, List<FunctionCatalogExtractor.PgFunction>> entry : catalog.entrySet())
        {
            if (entry.getValue().isEmpty())
            {
                continue;
            }
            String catHeadingAnchor = entry.getKey().toLowerCase().replace(" ", "-");
            md.append("<a id=\"").append(catHeadingAnchor).append("\"></a>\n\n");
            md.append("## ").append(entry.getKey()).append("\n\n");
            String docUrl = DocLinks.urlFor(entry.getKey());
            if (docUrl != null)
            {
                md.append("Reference: [PostgreSQL ").append(DocLinks.PG_DOCS_VERSION)
                        .append(" docs](").append(docUrl).append(")\n\n");
            }
            md.append("| | Function | Signature | TDS | Relation | Grammar | Error Category | Notes |\n");
            md.append("|--|----------|-----------|-----|----------|---------|----------------|-------|\n");

            for (FunctionCatalogExtractor.PgFunction fn : entry.getValue())
            {
                String tdsLabel = statusToLabel(fn.tdsStatus, fn.coverage, ParityPath.TDS);
                String relLabel = statusToLabel(fn.relStatus, fn.coverage, ParityPath.RELATION);
                String grammarLabel = statusToLabel(fn.grammarStatus, fn.coverage, ParityPath.GRAMMAR);
                String rowEmoji = rowColour(fn.tdsStatus, fn.relStatus, fn.grammarStatus);
                String sig = fn.signature.replace("|", "\\|");
                String errCat = getErrorCategory(fn, failureMap);
                String errLink = getErrorLink(fn, failureMap);

                md.append(String.format("| %s | `%s` | `%s` | %s | %s | %s | %s | %s |\n",
                        rowEmoji, fn.name, sig, tdsLabel, relLabel, grammarLabel,
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
                if ("NOT_APPLICABLE".equals(fn.tdsStatus) && fn.coverage != null)
                {
                    String errCat = getErrorCategory(fn, failureMap);
                    if (errCat.contains(ErrorCategorizer.FUNCTION_NOT_SUPPORTED)
                            || errCat.contains(ErrorCategorizer.FUNCTION_NO_SQL_TRANSLATION)
                            || errCat.contains(ErrorCategorizer.UNSUPPORTED_SYNTAX))
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
            md.append("They fail with \"No function matches the given name\", \"No SQL translation exists\", or \"Unsupported\" errors.\n\n");

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

        // === 8. Error Details — always last, so errors surface at the bottom of the report ===
        CoverageReportSupport.appendErrorDetailsSection(md, allErrors);

        try (FileWriter fw = new FileWriter(path))
        {
            fw.write(md.toString());
        }
    }

    private static String escapeForTable(String value)
    {
        if (value == null)
        {
            return "";
        }
        return value.replace("|", "\\|");
    }

    private static String rowColour(String tdsStatus, String relStatus, String grammarStatus)
    {
        List<String> statuses = new ArrayList<>();
        for (String status : new String[] {tdsStatus, relStatus, grammarStatus})
        {
            if (CoverageReportSupport.isRecorded(status))
            {
                statuses.add(status);
            }
        }
        if (statuses.isEmpty())
        {
            return "⚪";
        }
        boolean allPass = statuses.stream().allMatch("PASS"::equals);
        boolean allUnsupported = statuses.stream().allMatch("NOT_APPLICABLE"::equals);
        boolean allError = statuses.stream().allMatch(st -> "ERROR".equals(st) || "NOT_APPLICABLE".equals(st))
                && !allUnsupported;
        if (allPass)
        {
            return "\uD83D\uDFE2"; // 🟢
        }
        if (allUnsupported)
        {
            return "⚪"; // grey — unsupported
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
        return "⚪"; // untested/unknown
    }

    private static String statusToLabel(String status, FunctionCoverageMapper.SignatureCoverage cov, ParityPath path)
    {
        int pass = cov != null ? passFailError(cov, path)[0] : 0;
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

    private static String getErrorCategory(FunctionCatalogExtractor.PgFunction fn, Map<String, CoverageReportSupport.FailureInfo> failureMap)
    {
        return getErrorCategory(fn, failureMap, EnumSet.allOf(ParityPath.class));
    }

    private static String getErrorCategory(FunctionCatalogExtractor.PgFunction fn, Map<String, CoverageReportSupport.FailureInfo> failureMap, Set<ParityPath> paths)
    {
        if (paths.stream().allMatch(path -> "PASS".equals(statusOf(fn, path))))
        {
            return "";
        }
        if (paths.stream().allMatch(path -> "UNTESTED".equals(statusOf(fn, path))))
        {
            return "";
        }
        java.util.Set<String> categories = failureCategories(fn, failureMap, paths);
        if (!categories.isEmpty())
        {
            return String.join(", ", categories);
        }
        if (paths.stream().anyMatch(path -> "ERROR".equals(statusOf(fn, path))))
        {
            return ErrorCategorizer.MISC;
        }
        if (paths.stream().anyMatch(path -> "FAIL".equals(statusOf(fn, path))))
        {
            return ErrorCategorizer.RESULT_MISMATCH;
        }
        return "";
    }

    private static java.util.Set<String> failureCategories(FunctionCatalogExtractor.PgFunction fn, Map<String, CoverageReportSupport.FailureInfo> failureMap, Set<ParityPath> paths)
    {
        java.util.Set<String> categories = new java.util.LinkedHashSet<>();
        if (fn.coverage != null && !fn.coverage.testDetails.isEmpty())
        {
            for (FunctionCoverageMapper.TestResultEntry te : fn.coverage.testDetails)
            {
                for (ParityPath path : ParityPath.values())
                {
                    CoverageReportSupport.FailureInfo fi = failureMap.get(CoverageReportSupport.failureKey(te.testId, path));
                    if (fi != null && paths.contains(path))
                    {
                        categories.add(fi.category);
                    }
                }
            }
        }
        return categories;
    }

    private static String getErrorLink(FunctionCatalogExtractor.PgFunction fn, Map<String, CoverageReportSupport.FailureInfo> failureMap)
    {
        String cat = getErrorCategory(fn, failureMap);
        if (cat.isEmpty())
        {
            return "";
        }
        java.util.Set<String> categories = failureCategories(fn, failureMap, EnumSet.allOf(ParityPath.class));
        if (categories.isEmpty())
        {
            return cat;
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String category : categories)
        {
            if (!first)
            {
                sb.append(", ");
            }
            first = false;
            String anchor = findFirstAnchorForCategory(fn, failureMap, category);
            if (anchor != null)
            {
                sb.append(String.format("[%s](#%s)", category, anchor));
            }
            else
            {
                String catAnchor = category.toLowerCase().replace("_", "-");
                sb.append(String.format("[%s](#%s)", category, catAnchor));
            }
        }
        return sb.toString();
    }

    private static String findFirstAnchorForCategory(FunctionCatalogExtractor.PgFunction fn, Map<String, CoverageReportSupport.FailureInfo> failureMap, String category)
    {
        if (fn.coverage == null)
        {
            return null;
        }
        for (FunctionCoverageMapper.TestResultEntry te : fn.coverage.testDetails)
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

    private static void collectErrors(FunctionCatalogExtractor.PgFunction fn, Map<String, CoverageReportSupport.FailureInfo> failureMap, List<CoverageReportSupport.ErrorDetailEntry> allErrors)
    {
        if (fn.coverage == null)
        {
            return;
        }
        CoverageReportSupport.collectErrors(fn.coverage.testDetails, failureMap, allErrors);
    }

    private static void categorizeFunction(FunctionCatalogExtractor.PgFunction fn, Map<String, CoverageReportSupport.FailureInfo> failureMap, Map<String, int[]> counts)
    {
        if (fn.coverage == null)
        {
            return;
        }
        for (ParityPath path : ParityPath.values())
        {
            for (FunctionCoverageMapper.TestResultEntry te : fn.coverage.testDetails)
            {
                CoverageReportSupport.FailureInfo fi = failureMap.get(CoverageReportSupport.failureKey(te.testId, path));
                if (fi != null)
                {
                    counts.computeIfAbsent(fi.category, k -> new int[ParityPath.values().length])[path.ordinal()]++;
                    break;
                }
            }
        }
    }


    public void printConsoleSummary(Map<String, List<FunctionCatalogExtractor.PgFunction>> catalog)
    {
        int total = 0;
        Set<String> uniqueNames = new HashSet<>();
        for (List<FunctionCatalogExtractor.PgFunction> fns : catalog.values())
        {
            total += fns.size();
            for (FunctionCatalogExtractor.PgFunction fn : fns)
            {
                uniqueNames.add(fn.name);
            }
        }

        LOGGER.info("");
        LOGGER.info("═══════════════════════════════════════════════════════════════════════════");
        LOGGER.info("POSTGRES FUNCTION COVERAGE — Legend SQL (LegendSql)");
        LOGGER.info("═══════════════════════════════════════════════════════════════════════════");
        LOGGER.info(String.format("  Total signatures: %d", total));
        LOGGER.info(String.format("  Unique function names: %d", uniqueNames.size()));
        for (ParityPath path : ParityPath.values())
        {
            int pass = 0;
            int fail = 0;
            int error = 0;
            for (List<FunctionCatalogExtractor.PgFunction> fns : catalog.values())
            {
                for (FunctionCatalogExtractor.PgFunction fn : fns)
                {
                    if (fn.coverage != null && !"NOT_APPLICABLE".equals(statusOf(fn, path)))
                    {
                        int[] passFailError = passFailError(fn.coverage, path);
                        pass += passFailError[0];
                        fail += passFailError[1];
                        error += passFailError[2];
                    }
                }
            }
            LOGGER.info(String.format("  Total tests ran (%s): %d  (pass: %d, fail: %d, error: %d)",
                    consoleTestLabel(path), pass + fail + error, pass, fail, error));
        }
        for (ParityPath path : ParityPath.values())
        {
            int[] counts = new int[5];
            for (List<FunctionCatalogExtractor.PgFunction> fns : catalog.values())
            {
                int[] c = countStatuses(fns, path);
                for (int i = 0; i < 5; i++)
                {
                    counts[i] += c[i];
                }
            }
            LOGGER.info("");
            LOGGER.info("  " + path.label() + " Path:");
            LOGGER.info(String.format("    ✅ PASS:      %d", counts[0]));
            LOGGER.info(String.format("    ⚠️  PARTIAL:   %d", counts[1]));
            LOGGER.info(String.format("    ❌ FAIL:      %d  (result mismatch)", counts[2]));
            LOGGER.info(String.format("    💥 ERROR:     %d  (server exception)", counts[3]));
            LOGGER.info(String.format("    ❓ UNTESTED:  %d", counts[4]));
        }
        LOGGER.info("═══════════════════════════════════════════════════════════════════════════");
        LOGGER.info("");
    }

    private static String consoleTestLabel(ParityPath path)
    {
        return path == ParityPath.RELATION ? "Rel" : path.label();
    }

}
