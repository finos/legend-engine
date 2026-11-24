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

package org.finos.legend.engine.postgres.e2e.coverage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.postgres.e2e.ParityPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Generates a combined summary report (summary.md) pulling together the
 * headline numbers from both the function-coverage and structural-parity
 * reports, with overall support percentages.
 */
public class SummaryReport
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SummaryReport.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public void generate(String outputDir) throws IOException
    {
        File funcJson = new File(outputDir, "function-coverage.json");
        File opJson = new File(outputDir, "operator-coverage.json");
        File structJson = new File(outputDir, "structural-parity.json");
        File formatTokenJson = new File(outputDir, "format-token-coverage.json");

        if (!funcJson.exists() && !opJson.exists() && !structJson.exists() && !formatTokenJson.exists())
        {
            LOGGER.warn("No coverage JSON files found in {}, skipping summary report", outputDir);
            return;
        }

        StringBuilder md = new StringBuilder();
        md.append("# Legend SQL — Coverage Summary\n\n");

        Map<ParityPath, int[]> combinedByPath = new EnumMap<>(ParityPath.class);
        for (ParityPath path : ParityPath.values())
        {
            combinedByPath.put(path, new int[2]);
        }
        // Tracked separately from the per-path combined totals because format-token
        // coverage only has a single shared denominator (totalTokens) rather than
        // independent per-axis denominators, so it must only contribute once to the
        // overall/combined pool instead of once per axis.
        int combinedOverallPass = 0;
        int combinedOverallTotal = 0;

        // === Function Coverage ===
        if (funcJson.exists())
        {
            JsonNode root = MAPPER.readTree(funcJson);
            JsonNode summary = root.get("summary");
            int total = summary.get("total").asInt();
            int uniqueFunctionNames = summary.has("unique_function_names") ? summary.get("unique_function_names").asInt() : 0;
            int totalTests = summary.has("total_tests") ? summary.get("total_tests").asInt() : 0;
            Map<ParityPath, PathSummary> byPath = pathSummaries(summary);
            Map<ParityPath, Integer> supported = new EnumMap<>(ParityPath.class);
            for (ParityPath path : ParityPath.values())
            {
                String key = path.reportKey() + "_supported_functions";
                supported.put(path, summary.has(key) ? summary.get(key).asInt() : 0);
            }

            md.append("## Function Coverage\n\n");
            md.append("Coverage of Postgres built-in functions in Legend SQL.\n");
            md.append("See [full details](function-coverage.md) for per-function results.\n\n");

            appendMetricsHeader(md);
            appendRow(md, "Total signatures", path -> String.valueOf(total));
            appendRow(md, "Unique function names", path -> String.valueOf(uniqueFunctionNames));
            appendRow(md, "**Supported functions (PASS/PARTIAL)**", path -> String.format("**%d (%.1f%%)**", supported.get(path),
                    uniqueFunctionNames > 0 ? (100.0 * supported.get(path) / uniqueFunctionNames) : 0));
            appendRow(md, "Total tests", path -> String.valueOf(totalTests));
            appendRow(md, "⚪ UNSUPPORTED", path -> String.valueOf(byPath.get(path).unsupported));
            appendRow(md, "✅ PASS", path -> String.valueOf(byPath.get(path).pass));
            appendRow(md, "⚠️ PARTIAL", path -> String.valueOf(byPath.get(path).partial));
            appendRow(md, "❌ FAIL", path -> String.valueOf(byPath.get(path).fail));
            appendRow(md, "💥 ERROR", path -> String.valueOf(byPath.get(path).error));
            appendRow(md, "❓ UNTESTED", path -> String.valueOf(byPath.get(path).untested));
            appendRow(md, "**Full pass rate**", path -> String.format("**%.1f%%**", byPath.get(path).passPct()));
            appendRow(md, "**Pass + partial rate**", path -> String.format("**%.1f%%**", byPath.get(path).passPartialPct()));
            md.append("\n_Percentages exclude unsupported functions from the denominator._\n");

            // List FAIL test links
            appendFailLinks(md, root, "function");

            md.append("\n---\n\n");

            for (ParityPath path : ParityPath.values())
            {
                combinedByPath.get(path)[0] += byPath.get(path).pass;
                combinedByPath.get(path)[1] += byPath.get(path).expected();
                combinedOverallPass += byPath.get(path).pass;
                combinedOverallTotal += byPath.get(path).expected();
            }
        }

        // === Operator Coverage ===
        if (opJson.exists())
        {
            JsonNode root = MAPPER.readTree(opJson);
            JsonNode summary = root.get("summary");
            int total = summary.get("total").asInt();
            Map<ParityPath, PathSummary> byPath = pathSummaries(summary);

            md.append("## Operator Coverage\n\n");
            md.append("Coverage of Postgres built-in operators in Legend SQL.\n");
            md.append("See [full details](operator-coverage.md) for per-operator results.\n\n");

            appendMetricsHeader(md);
            appendRow(md, "Total operators", path -> String.valueOf(total));
            appendRow(md, "⚪ UNSUPPORTED", path -> String.valueOf(byPath.get(path).unsupported));
            appendRow(md, "✅ PASS", path -> String.valueOf(byPath.get(path).pass));
            appendRow(md, "⚠️ PARTIAL", path -> String.valueOf(byPath.get(path).partial));
            appendRow(md, "❌ FAIL", path -> String.valueOf(byPath.get(path).fail));
            appendRow(md, "💥 ERROR", path -> String.valueOf(byPath.get(path).error));
            appendRow(md, "❓ UNTESTED", path -> String.valueOf(byPath.get(path).untested));
            appendRow(md, "**Full pass rate**", path -> String.format("**%.1f%%**", byPath.get(path).passPct()));

            // List FAIL test links
            appendFailLinks(md, root, "operator");

            md.append("\n---\n\n");

            for (ParityPath path : ParityPath.values())
            {
                combinedByPath.get(path)[0] += byPath.get(path).pass;
                combinedByPath.get(path)[1] += byPath.get(path).expected();
                combinedOverallPass += byPath.get(path).pass;
                combinedOverallTotal += byPath.get(path).expected();
            }
        }

        // === Structural Parity ===
        if (structJson.exists())
        {
            JsonNode root = MAPPER.readTree(structJson);
            JsonNode summary = root.get("summary");
            int totalFeatures = summary.get("total_features").asInt();
            int totalTests = summary.get("total_tests").asInt();
            Map<ParityPath, PathSummary> byPath = pathSummaries(summary);

            md.append("## Structural Parity\n\n");
            md.append("Coverage of SQL structural features (joins, subqueries, aggregations, etc.).\n");
            md.append("See [full details](structural-parity.md) for per-feature results.\n\n");

            appendMetricsHeader(md);
            appendRow(md, "Total features", path -> String.valueOf(totalFeatures));
            appendRow(md, "Total tests", path -> String.valueOf(totalTests));
            appendRow(md, "⚪ UNSUPPORTED", path -> String.valueOf(byPath.get(path).unsupported));
            appendRow(md, "✅ PASS", path -> String.valueOf(byPath.get(path).pass));
            appendRow(md, "⚠️ PARTIAL", path -> String.valueOf(byPath.get(path).partial));
            appendRow(md, "❌ FAIL", path -> String.valueOf(byPath.get(path).fail));
            appendRow(md, "💥 ERROR", path -> String.valueOf(byPath.get(path).error));
            appendRow(md, "❓ UNTESTED", path -> String.valueOf(byPath.get(path).untested));
            appendRow(md, "**Full pass rate**", path -> String.format("**%.1f%%**", byPath.get(path).passPct()));
            appendRow(md, "**Pass + partial rate**", path -> String.format("**%.1f%%**", byPath.get(path).passPartialPct()));
            md.append("\n_Percentages exclude unsupported features from the denominator._\n");

            // List FAIL test links
            appendFailLinks(md, root, "structural");

            md.append("\n---\n\n");

            for (ParityPath path : ParityPath.values())
            {
                combinedByPath.get(path)[0] += byPath.get(path).pass;
                combinedByPath.get(path)[1] += byPath.get(path).expected();
                combinedOverallPass += byPath.get(path).pass;
                combinedOverallTotal += byPath.get(path).expected();
            }
        }

        // === Format Token Coverage ===
        if (formatTokenJson.exists())
        {
            JsonNode root = MAPPER.readTree(formatTokenJson);
            JsonNode summary = root.get("summary");
            int totalTokens = summary.get("total_tokens").asInt();
            Map<ParityPath, Integer> passByPath = new EnumMap<>(ParityPath.class);
            for (ParityPath path : ParityPath.values())
            {
                String key = path.reportKey() + "_pass";
                passByPath.put(path, summary.has(key) ? summary.get(key).asInt() : 0);
            }

            md.append("## Format Token Coverage\n\n");
            md.append("Coverage of `to_char` template patterns and `EXTRACT`/`date_part` field keywords.\n");
            md.append("See [full details](format-token-coverage.md) for per-token results.\n\n");

            appendMetricsHeader(md);
            appendRow(md, "Total tokens", path -> String.valueOf(totalTokens));
            appendRow(md, "✅ PASS", path -> String.valueOf(passByPath.get(path)));
            appendRow(md, "**Pass rate**", path -> String.format("**%.1f%%**",
                    totalTokens > 0 ? (100.0 * passByPath.get(path) / totalTokens) : 0));
            md.append("\n---\n\n");

            for (ParityPath path : ParityPath.values())
            {
                combinedByPath.get(path)[0] += passByPath.get(path);
                combinedByPath.get(path)[1] += totalTokens;
                combinedOverallPass += passByPath.get(path);
            }
            combinedOverallTotal += totalTokens;
        }

        // === Overall ===
        double overallPct = combinedOverallTotal > 0
                ? (100.0 * combinedOverallPass / combinedOverallTotal) : 0;

        appendDocsCoverageSection(md, funcJson, opJson);

        md.append("## Overall\n\n");
        md.append("Combined pass rate across function coverage and structural parity.\n\n");
        md.append("| Path | Pass | Tested | Pass Rate |\n");
        md.append("|------|------|--------|-----------|\n");
        for (ParityPath path : ParityPath.values())
        {
            int[] combined = combinedByPath.get(path);
            double pct = combined[1] > 0 ? (100.0 * combined[0] / combined[1]) : 0;
            md.append(String.format("| %s | %d | %d | **%.1f%%** |\n", path.label(), combined[0], combined[1], pct));
        }
        md.append(String.format("| **Combined** | **%d** | **%d** | **%.1f%%** |\n",
                combinedOverallPass, combinedOverallTotal, overallPct));
        md.append("\n");

        String filePath = outputDir + "/summary.md";
        try (FileWriter fw = new FileWriter(filePath))
        {
            fw.write(md.toString());
        }
        LOGGER.info("Generated summary report: {}", filePath);
    }

    /**
     * Appends a "Coverage vs. Postgres docs" section: per doc section (9.1-9.27), the
     * fraction of catalogued functions/operators with *any* test at all, independent of
     * PASS/FAIL — makes visible where there is a testing gap vs. a behavioral gap
     * (gap-analysis section 3.4.3).
     */
    private void appendDocsCoverageSection(StringBuilder md, File funcJson, File opJson) throws IOException
    {
        java.util.Map<String, int[]> perCategory = new java.util.LinkedHashMap<>(); // [testedCount, total]

        for (File jsonFile : new File[] {funcJson, opJson})
        {
            if (!jsonFile.exists())
            {
                continue;
            }
            JsonNode root = MAPPER.readTree(jsonFile);
            JsonNode categories = root.get("categories");
            if (categories == null || !categories.isArray())
            {
                continue;
            }
            for (JsonNode cat : categories)
            {
                String name = cat.has("name") ? cat.get("name").asText() : "Unknown";
                JsonNode items = cat.get("functions");
                if (items == null)
                {
                    items = cat.get("operators");
                }
                if (items == null)
                {
                    continue;
                }
                int[] counts = perCategory.computeIfAbsent(name, k -> new int[2]);
                for (JsonNode item : items)
                {
                    counts[1]++;
                    boolean anyPathTested = false;
                    for (ParityPath path : ParityPath.values())
                    {
                        if (!"UNTESTED".equals(item.path(path.reportKey()).path("status").asText("UNTESTED")))
                        {
                            anyPathTested = true;
                        }
                    }
                    if (anyPathTested)
                    {
                        counts[0]++;
                    }
                }
            }
        }

        if (perCategory.isEmpty())
        {
            return;
        }

        md.append("## Coverage vs. Postgres Docs\n\n");
        md.append("Fraction of cataloged functions/operators with *any* test at all — independent of\n");
        md.append("pass/fail — showing where there is a testing gap vs. a behavioral gap.\n\n");
        md.append("| Doc Section | Tested | Total | Tested % |\n");
        md.append("|---|---|---|---|\n");
        for (java.util.Map.Entry<String, int[]> entry : perCategory.entrySet())
        {
            int[] c = entry.getValue();
            double pct = c[1] > 0 ? (100.0 * c[0] / c[1]) : 0;
            md.append(String.format("| %s | %d | %d | %.1f%% |\n",
                    DocLinks.linkedHeading(entry.getKey()), c[0], c[1], pct));
        }
        md.append("\n---\n\n");
    }

    /**
     * Appends a list of links to failure-details.md for every FAIL test found in the JSON report.
     */
    private void appendFailLinks(StringBuilder md, JsonNode root, String reportType)
    {
        JsonNode categories = root.get("categories");
        if (categories == null || !categories.isArray())
        {
            return;
        }

        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        for (JsonNode cat : categories)
        {
            // Try "functions" (function coverage), "operators" (operator coverage), or "features" (structural parity)
            JsonNode items = cat.get("functions");
            if (items == null)
            {
                items = cat.get("operators");
            }
            if (items == null)
            {
                items = cat.get("features");
            }
            if (items == null)
            {
                continue;
            }
            
            for (JsonNode item : items)
            {
                JsonNode details = item.get("testDetails");
                if (details == null)
                {
                    continue;
                }
                for (JsonNode te : details)
                {
                    String testId = te.get("id").asText();
                    for (ParityPath path : ParityPath.values())
                    {
                        String state = te.has(path.reportKey()) ? te.get(path.reportKey()).asText() : "";
                        if ("FAIL".equals(state))
                        {
                            String anchor = "fail-" + testId + "-" + path.label();
                            String link = String.format("- [%s \\[%s\\]](failure-details.md#%s)\n", testId, path.label(), anchor);
                            seen.add(link);
                        }
                    }
                }
            }
        }

        if (!seen.isEmpty())
        {
            md.append("\n**Failed tests:**\n\n");
            for (String link : seen)
            {
                md.append(link);
            }
        }
    }

    private static void appendMetricsHeader(StringBuilder md)
    {
        md.append("| Metric | TDS | Relation | Grammar |\n");
        md.append("|--------|-----|----------|---------|\n");
    }

    private static void appendRow(StringBuilder md, String label, Function<ParityPath, String> cell)
    {
        md.append("| ").append(label);
        for (ParityPath path : ParityPath.values())
        {
            md.append(" | ").append(cell.apply(path));
        }
        md.append(" |\n");
    }

    private static Map<ParityPath, PathSummary> pathSummaries(JsonNode summary)
    {
        Map<ParityPath, PathSummary> byPath = new EnumMap<>(ParityPath.class);
        for (ParityPath path : ParityPath.values())
        {
            byPath.put(path, new PathSummary(summary.path(path.reportKey())));
        }
        return byPath;
    }

    private static final class PathSummary
    {
        private final int pass;
        private final int partial;
        private final int fail;
        private final int error;
        private final int untested;
        private final int unsupported;

        private PathSummary(JsonNode node)
        {
            this.pass = node.path("pass").asInt();
            this.partial = node.path("partial").asInt();
            this.fail = node.path("fail").asInt();
            this.error = node.path("error").asInt();
            this.untested = node.path("untested").asInt();
            this.unsupported = node.path("unsupported").asInt();
        }

        private int expected()
        {
            return pass + partial + fail + error + untested;
        }

        private double passPct()
        {
            return expected() > 0 ? (100.0 * pass / expected()) : 0;
        }

        private double passPartialPct()
        {
            return expected() > 0 ? (100.0 * (pass + partial) / expected()) : 0;
        }
    }
}
