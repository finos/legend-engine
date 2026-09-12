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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates operator coverage reports in JSON and Markdown format, mirroring
 * {@link FunctionCoverageReport} (and sharing its rendering/classification building blocks via
 * {@link CoverageReportSupport}). Every catalogued-but-untested operator is reported as UNTESTED,
 * distinguishing "no test exists" from "tests exist but fail".
 */
public class OperatorCoverageReport
{
    private static final Logger LOGGER = LoggerFactory.getLogger(OperatorCoverageReport.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public void generate(Map<String, List<OperatorCatalogExtractor.PgOperator>> catalog, String outputDir, File parityReportFile) throws IOException
    {
        new File(outputDir).mkdirs();
        generateJson(catalog, outputDir + "/operator-coverage.json");
        generateMarkdown(catalog, outputDir + "/operator-coverage.md", parityReportFile);
        printConsoleSummary(catalog);
    }

    private static int[] countStatuses(Iterable<OperatorCatalogExtractor.PgOperator> ops, boolean tds)
    {
        // [pass, partial, fail, error, untested]
        int[] c = new int[5];
        for (OperatorCatalogExtractor.PgOperator op : ops)
        {
            String s = tds ? op.tdsStatus : op.relStatus;
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

    private void generateJson(Map<String, List<OperatorCatalogExtractor.PgOperator>> catalog, String path) throws IOException
    {
        ObjectNode root = MAPPER.createObjectNode();

        int total = 0;
        int[] tds = new int[5];
        int[] rel = new int[5];
        for (List<OperatorCatalogExtractor.PgOperator> ops : catalog.values())
        {
            total += ops.size();
            int[] t = countStatuses(ops, true);
            int[] r = countStatuses(ops, false);
            for (int i = 0; i < 5; i++)
            {
                tds[i] += t[i];
                rel[i] += r[i];
            }
        }

        ObjectNode summary = root.putObject("summary");
        summary.put("total", total);
        summary.put("pg_docs_version", OperatorCatalogExtractor.PG_DOCS_VERSION);
        putPathSummary(summary.putObject("tds"), tds, total);
        putPathSummary(summary.putObject("relation"), rel, total);

        ArrayNode categories = root.putArray("categories");
        for (Map.Entry<String, List<OperatorCatalogExtractor.PgOperator>> entry : catalog.entrySet())
        {
            if (entry.getValue().isEmpty())
            {
                continue;
            }
            ObjectNode catNode = categories.addObject();
            catNode.put("name", entry.getKey());

            ArrayNode operators = catNode.putArray("operators");
            for (OperatorCatalogExtractor.PgOperator op : entry.getValue())
            {
                ObjectNode opNode = operators.addObject();
                opNode.put("name", op.name);
                opNode.put("signature", op.signature);
                opNode.put("kind", op.kind);

                ObjectNode tdsNode = opNode.putObject("tds");
                tdsNode.put("status", op.tdsStatus);
                ObjectNode relNode = opNode.putObject("relation");
                relNode.put("status", op.relStatus);
                if (op.coverage != null)
                {
                    tdsNode.put("pass", op.coverage.tdsPass);
                    tdsNode.put("fail", op.coverage.tdsFail);
                    tdsNode.put("error", op.coverage.tdsError);
                    tdsNode.put("total", op.coverage.total);
                    relNode.put("pass", op.coverage.relPass);
                    relNode.put("fail", op.coverage.relFail);
                    relNode.put("error", op.coverage.relError);
                    relNode.put("total", op.coverage.total);
                }
                if (!op.notes.isEmpty())
                {
                    opNode.put("notes", op.notes);
                }
            }
        }

        MAPPER.writeValue(new File(path), root);
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

    private void generateMarkdown(Map<String, List<OperatorCatalogExtractor.PgOperator>> catalog, String path, File parityReportFile) throws IOException
    {
        // Load error details from parity report
        Map<String, CoverageReportSupport.FailureInfo> failureMap = CoverageReportSupport.loadFailures(parityReportFile);

        // Reclassify operators whose only failures are "unsupported" style errors (function not
        // recognized / unsupported syntax) as NOT_APPLICABLE (unsupported, not error) — same
        // convention as FunctionCoverageReport.
        for (List<OperatorCatalogExtractor.PgOperator> ops : catalog.values())
        {
            for (OperatorCatalogExtractor.PgOperator op : ops)
            {
                String errCat = getErrorCategory(op, failureMap);
                if (errCat.contains(ErrorCategorizer.FUNCTION_NOT_SUPPORTED))
                {
                    op.tdsStatus = "NOT_APPLICABLE";
                    op.relStatus = "NOT_APPLICABLE";
                    if (op.notes.isEmpty())
                    {
                        op.notes = "Operator not supported";
                    }
                }
                else if (errCat.contains(ErrorCategorizer.UNSUPPORTED_SYNTAX))
                {
                    op.tdsStatus = "NOT_APPLICABLE";
                    op.relStatus = "NOT_APPLICABLE";
                    if (op.notes.isEmpty())
                    {
                        op.notes = "Unsupported feature";
                    }
                }
            }
        }

        StringBuilder md = new StringBuilder();
        md.append("# Postgres Operator Coverage — Legend SQL (LegendSql)\n\n");
        md.append("Reference: [PostgreSQL ").append(OperatorCatalogExtractor.PG_DOCS_VERSION)
                .append(" Functions and Operators](https://www.postgresql.org/docs/")
                .append(OperatorCatalogExtractor.PG_DOCS_VERSION).append("/functions.html)\n\n");

        // === 1. Summary ===
        int total = 0;
        int[] tds = new int[5];
        int[] rel = new int[5];
        for (List<OperatorCatalogExtractor.PgOperator> ops : catalog.values())
        {
            total += ops.size();
            int[] t = countStatuses(ops, true);
            int[] r = countStatuses(ops, false);
            for (int i = 0; i < 5; i++)
            {
                tds[i] += t[i];
                rel[i] += r[i];
            }
        }

        md.append("## Summary\n\n");
        md.append("| Path | PASS | PARTIAL | FAIL | ERROR | UNTESTED | UNSUPPORTED | Total |\n");
        md.append("|---|---|---|---|---|---|---|---|\n");
        appendSummaryRow(md, "TDS", tds, total);
        appendSummaryRow(md, "Relation", rel, total);
        md.append("\n---\n\n");

        // === 2. Error Categories ===
        Map<String, int[]> errorCategoryCounts = new LinkedHashMap<>();
        for (List<OperatorCatalogExtractor.PgOperator> ops : catalog.values())
        {
            for (OperatorCatalogExtractor.PgOperator op : ops)
            {
                categorizeOperator(op, failureMap, errorCategoryCounts);
            }
        }
        CoverageReportSupport.appendErrorCategoriesSection(md, errorCategoryCounts);

        // === 3. Category Summary ===
        md.append("## Category Summary\n\n");
        md.append("| Category | Total | TDS PASS | TDS PARTIAL | TDS FAIL | TDS ERROR | TDS UNTESTED | Rel PASS | Rel PARTIAL | Rel FAIL | Rel ERROR | Rel UNTESTED |\n");
        md.append("|---|---|---|---|---|---|---|---|---|---|---|---|\n");
        for (Map.Entry<String, List<OperatorCatalogExtractor.PgOperator>> entry : catalog.entrySet())
        {
            if (entry.getValue().isEmpty())
            {
                continue;
            }
            int ct = entry.getValue().size();
            int[] t = countStatuses(entry.getValue(), true);
            int[] r = countStatuses(entry.getValue(), false);
            md.append(String.format("| %s | %d | %d | %d | %d | %d | %d | %d | %d | %d | %d | %d |\n",
                    anchorLink(entry.getKey()), ct,
                    t[0], t[1], t[2], t[3], t[4],
                    r[0], r[1], r[2], r[3], r[4]));
        }
        md.append("\n---\n\n");

        // === 4. Per-category operator results ===
        List<CoverageReportSupport.ErrorDetailEntry> allErrors = new ArrayList<>();

        for (Map.Entry<String, List<OperatorCatalogExtractor.PgOperator>> entry : catalog.entrySet())
        {
            if (entry.getValue().isEmpty())
            {
                continue;
            }
            md.append("## ").append(entry.getKey()).append("\n\n");
            String docUrl = DocLinks.urlFor(entry.getKey());
            if (docUrl != null)
            {
                md.append("Reference: [PostgreSQL ").append(OperatorCatalogExtractor.PG_DOCS_VERSION)
                        .append(" docs](").append(docUrl).append(")\n\n");
            }
            md.append("| | Operator | Signature | TDS | Relation | Error Category | Notes |\n");
            md.append("|--|---|---|---|---|---|---|\n");
            for (OperatorCatalogExtractor.PgOperator op : entry.getValue())
            {
                int tdsTotal = op.coverage == null ? 0 : op.coverage.total - op.coverage.tdsSkip;
                int relTotal = op.coverage == null ? 0 : op.coverage.total - op.coverage.relSkip;
                int tdsPass = op.coverage == null ? 0 : op.coverage.tdsPass;
                int relPass = op.coverage == null ? 0 : op.coverage.relPass;
                String errCat = getErrorCategory(op, failureMap);
                String errLink = getErrorLink(op, failureMap);
                md.append("| ").append(CoverageReportSupport.rowColour(op.tdsStatus, op.relStatus))
                        .append(" | `").append(op.name).append("` | `").append(op.signature).append("` | ")
                        .append(CoverageReportSupport.statusToLabel(op.tdsStatus, tdsPass, tdsTotal)).append(" | ")
                        .append(CoverageReportSupport.statusToLabel(op.relStatus, relPass, relTotal)).append(" | ")
                        .append(errCat.isEmpty() ? "" : errLink).append(" | ")
                        .append(op.notes).append(" |\n");

                if (op.coverage != null)
                {
                    CoverageReportSupport.collectErrors(op.coverage.testDetails, failureMap, allErrors);
                }
            }
            md.append("\n");
        }

        md.append("---\n\n");

        // === 5. Error Details — always last, so errors surface at the bottom of the report ===
        CoverageReportSupport.appendErrorDetailsSection(md, allErrors);

        try (FileWriter writer = new FileWriter(path))
        {
            writer.write(md.toString());
        }
    }

    private static String getErrorCategory(OperatorCatalogExtractor.PgOperator op, Map<String, CoverageReportSupport.FailureInfo> failureMap)
    {
        if ("PASS".equals(op.tdsStatus) && "PASS".equals(op.relStatus))
        {
            return "";
        }
        if ("UNTESTED".equals(op.tdsStatus) && "UNTESTED".equals(op.relStatus))
        {
            return "";
        }
        List<FunctionCoverageMapper.TestResultEntry> testDetails = op.coverage != null ? op.coverage.testDetails : null;
        java.util.LinkedHashSet<String> categories = CoverageReportSupport.categoriesFor(testDetails, failureMap);
        if (!categories.isEmpty())
        {
            return String.join(", ", categories);
        }
        // Infer from status when no parity-report detail is available
        if ("ERROR".equals(op.tdsStatus) || "ERROR".equals(op.relStatus))
        {
            return ErrorCategorizer.MISC;
        }
        if ("FAIL".equals(op.tdsStatus) || "FAIL".equals(op.relStatus))
        {
            return ErrorCategorizer.RESULT_MISMATCH;
        }
        return "";
    }

    private static String getErrorLink(OperatorCatalogExtractor.PgOperator op, Map<String, CoverageReportSupport.FailureInfo> failureMap)
    {
        if (getErrorCategory(op, failureMap).isEmpty())
        {
            return "";
        }
        List<FunctionCoverageMapper.TestResultEntry> testDetails = op.coverage != null ? op.coverage.testDetails : null;
        return CoverageReportSupport.errorCategoryLink(testDetails, failureMap);
    }

    private static void categorizeOperator(OperatorCatalogExtractor.PgOperator op, Map<String, CoverageReportSupport.FailureInfo> failureMap, Map<String, int[]> counts)
    {
        String tdsCategory = null;
        String relCategory = null;

        if (op.coverage != null)
        {
            for (FunctionCoverageMapper.TestResultEntry te : op.coverage.testDetails)
            {
                if (tdsCategory == null)
                {
                    CoverageReportSupport.FailureInfo fi = failureMap.get(te.testId + "|TDS");
                    if (fi != null)
                    {
                        tdsCategory = fi.category;
                    }
                }
                if (relCategory == null)
                {
                    CoverageReportSupport.FailureInfo fi = failureMap.get(te.testId + "|Relation");
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

    /**
     * Renders a category heading as a markdown link to its own section further down in this same
     * report (rather than out to the external PostgreSQL docs), using GitHub's heading-anchor
     * slug algorithm: lower-case, strip anything but letters/digits/spaces/hyphens, then turn
     * spaces into hyphens.
     */
    private static String anchorLink(String category)
    {
        return "[" + category + "](#" + slug(category) + ")";
    }

    private static String slug(String heading)
    {
        String s = heading.toLowerCase().replaceAll("[^a-z0-9\\s-]", "");
        return s.trim().replaceAll("\\s+", "-");
    }

    private static void appendSummaryRow(StringBuilder md, String label, int[] counts, int total)
    {
        int accounted = counts[0] + counts[1] + counts[2] + counts[3] + counts[4];
        int unsupported = total - accounted;
        md.append("| ").append(label).append(" | ").append(counts[0]).append(" | ").append(counts[1])
                .append(" | ").append(counts[2]).append(" | ").append(counts[3]).append(" | ")
                .append(counts[4]).append(" | ").append(unsupported).append(" | ").append(total).append(" |\n");
    }

    private void printConsoleSummary(Map<String, List<OperatorCatalogExtractor.PgOperator>> catalog)
    {
        int total = 0;
        int tdsPass = 0;
        for (List<OperatorCatalogExtractor.PgOperator> ops : catalog.values())
        {
            total += ops.size();
            for (OperatorCatalogExtractor.PgOperator op : ops)
            {
                if ("PASS".equals(op.tdsStatus))
                {
                    tdsPass++;
                }
            }
        }
        LOGGER.info("Operator coverage: {}/{} operators PASS on TDS path", tdsPass, total);
    }
}

