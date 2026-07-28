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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

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
        File structJson = new File(outputDir, "structural-parity.json");

        if (!funcJson.exists() && !structJson.exists())
        {
            LOGGER.warn("No coverage JSON files found in {}, skipping summary report", outputDir);
            return;
        }

        StringBuilder md = new StringBuilder();
        md.append("# Legend SQL — Coverage Summary\n\n");

        int combinedTdsPass = 0;
        int combinedRelPass = 0;
        int combinedTdsTotal = 0;
        int combinedRelTotal = 0;

        // === Function Coverage ===
        if (funcJson.exists())
        {
            JsonNode root = MAPPER.readTree(funcJson);
            JsonNode summary = root.get("summary");
            int total = summary.get("total").asInt();
            int uniqueFunctionNames = summary.has("unique_function_names") ? summary.get("unique_function_names").asInt() : 0;
            int tdsSupported = summary.has("tds_supported_functions") ? summary.get("tds_supported_functions").asInt() : 0;
            int relSupported = summary.has("relation_supported_functions") ? summary.get("relation_supported_functions").asInt() : 0;
            int totalTests = summary.has("total_tests") ? summary.get("total_tests").asInt() : 0;
            double tdsSupportedPct = uniqueFunctionNames > 0 ? (100.0 * tdsSupported / uniqueFunctionNames) : 0;
            double relSupportedPct = uniqueFunctionNames > 0 ? (100.0 * relSupported / uniqueFunctionNames) : 0;
            
            JsonNode tds = summary.get("tds");
            JsonNode rel = summary.get("relation");

            int tdsPass = tds.get("pass").asInt();
            int tdsPartial = tds.get("partial").asInt();
            int tdsFail = tds.get("fail").asInt();
            int tdsError = tds.get("error").asInt();
            int tdsUntested = tds.get("untested").asInt();
            int tdsUnsupported = tds.has("unsupported") ? tds.get("unsupported").asInt() : 0;
            int tdsExpected = tdsPass + tdsPartial + tdsFail + tdsError + tdsUntested;

            int relPass = rel.get("pass").asInt();
            int relPartial = rel.get("partial").asInt();
            int relFail = rel.get("fail").asInt();
            int relError = rel.get("error").asInt();
            int relUntested = rel.get("untested").asInt();
            int relUnsupported = rel.has("unsupported") ? rel.get("unsupported").asInt() : 0;
            int relExpected = relPass + relPartial + relFail + relError + relUntested;

            double tdsPct = tdsExpected > 0 ? (100.0 * tdsPass / tdsExpected) : 0;
            double relPct = relExpected > 0 ? (100.0 * relPass / relExpected) : 0;
            double tdsPartialPct = tdsExpected > 0 ? (100.0 * (tdsPass + tdsPartial) / tdsExpected) : 0;
            double relPartialPct = relExpected > 0 ? (100.0 * (relPass + relPartial) / relExpected) : 0;

            md.append("## Function Coverage\n\n");
            md.append("Coverage of Postgres built-in functions in Legend SQL.\n");
            md.append("See [full details](function-coverage.md) for per-function results.\n\n");

            md.append("| Metric | TDS | Relation |\n");
            md.append("|--------|-----|----------|\n");
            md.append(String.format("| Total signatures | %d | %d |\n", total, total));
            md.append(String.format("| Unique function names | %d | %d |\n", uniqueFunctionNames, uniqueFunctionNames));
            md.append(String.format("| **Supported functions (PASS/PARTIAL)** | **%d (%.1f%%)** | **%d (%.1f%%)** |\n", 
                    tdsSupported, tdsSupportedPct, relSupported, relSupportedPct));
            md.append(String.format("| Total tests | %d | %d |\n", totalTests, totalTests));
            md.append(String.format("| ⚪ UNSUPPORTED | %d | %d |\n", tdsUnsupported, relUnsupported));
            md.append(String.format("| ✅ PASS | %d | %d |\n", tdsPass, relPass));
            md.append(String.format("| ⚠️ PARTIAL | %d | %d |\n", tdsPartial, relPartial));
            md.append(String.format("| ❌ FAIL | %d | %d |\n", tdsFail, relFail));
            md.append(String.format("| 💥 ERROR | %d | %d |\n", tdsError, relError));
            md.append(String.format("| ❓ UNTESTED | %d | %d |\n", tdsUntested, relUntested));
            md.append(String.format("| **Full pass rate** | **%.1f%%** | **%.1f%%** |\n", tdsPct, relPct));
            md.append(String.format("| **Pass + partial rate** | **%.1f%%** | **%.1f%%** |\n", tdsPartialPct, relPartialPct));
            md.append("\n_Percentages exclude unsupported functions from the denominator._\n");

            // List FAIL test links
            appendFailLinks(md, root, "function");

            md.append("\n---\n\n");

            combinedTdsPass += tdsPass;
            combinedRelPass += relPass;
            combinedTdsTotal += tdsExpected;
            combinedRelTotal += relExpected;
        }

        // === Structural Parity ===
        if (structJson.exists())
        {
            JsonNode root = MAPPER.readTree(structJson);
            JsonNode summary = root.get("summary");
            int totalFeatures = summary.get("total_features").asInt();
            int totalTests = summary.get("total_tests").asInt();
            JsonNode tds = summary.get("tds");
            JsonNode rel = summary.get("relation");

            int tdsPass = tds.get("pass").asInt();
            int tdsPartial = tds.get("partial").asInt();
            int tdsFail = tds.get("fail").asInt();
            int tdsError = tds.get("error").asInt();
            int tdsUntested = tds.get("untested").asInt();
            int tdsUnsupported = tds.has("unsupported") ? tds.get("unsupported").asInt() : 0;
            int tdsExpected = tdsPass + tdsPartial + tdsFail + tdsError + tdsUntested;

            int relPass = rel.get("pass").asInt();
            int relPartial = rel.get("partial").asInt();
            int relFail = rel.get("fail").asInt();
            int relError = rel.get("error").asInt();
            int relUntested = rel.get("untested").asInt();
            int relUnsupported = rel.has("unsupported") ? rel.get("unsupported").asInt() : 0;
            int relExpected = relPass + relPartial + relFail + relError + relUntested;

            double tdsPct = tdsExpected > 0 ? (100.0 * tdsPass / tdsExpected) : 0;
            double relPct = relExpected > 0 ? (100.0 * relPass / relExpected) : 0;
            double tdsPartialPct = tdsExpected > 0 ? (100.0 * (tdsPass + tdsPartial) / tdsExpected) : 0;
            double relPartialPct = relExpected > 0 ? (100.0 * (relPass + relPartial) / relExpected) : 0;

            md.append("## Structural Parity\n\n");
            md.append("Coverage of SQL structural features (joins, subqueries, aggregations, etc.).\n");
            md.append("See [full details](structural-parity.md) for per-feature results.\n\n");

            md.append("| Metric | TDS | Relation |\n");
            md.append("|--------|-----|----------|\n");
            md.append(String.format("| Total features | %d | %d |\n", totalFeatures, totalFeatures));
            md.append(String.format("| Total tests | %d | %d |\n", totalTests, totalTests));
            md.append(String.format("| ⚪ UNSUPPORTED | %d | %d |\n", tdsUnsupported, relUnsupported));
            md.append(String.format("| ✅ PASS | %d | %d |\n", tdsPass, relPass));
            md.append(String.format("| ⚠️ PARTIAL | %d | %d |\n", tdsPartial, relPartial));
            md.append(String.format("| ❌ FAIL | %d | %d |\n", tdsFail, relFail));
            md.append(String.format("| 💥 ERROR | %d | %d |\n", tdsError, relError));
            md.append(String.format("| ❓ UNTESTED | %d | %d |\n", tdsUntested, relUntested));
            md.append(String.format("| **Full pass rate** | **%.1f%%** | **%.1f%%** |\n", tdsPct, relPct));
            md.append(String.format("| **Pass + partial rate** | **%.1f%%** | **%.1f%%** |\n", tdsPartialPct, relPartialPct));
            md.append("\n_Percentages exclude unsupported features from the denominator._\n");

            // List FAIL test links
            appendFailLinks(md, root, "structural");

            md.append("\n---\n\n");

            combinedTdsPass += tdsPass;
            combinedRelPass += relPass;
            combinedTdsTotal += tdsExpected;
            combinedRelTotal += relExpected;
        }

        // === Overall ===
        double overallTdsPct = combinedTdsTotal > 0 ? (100.0 * combinedTdsPass / combinedTdsTotal) : 0;
        double overallRelPct = combinedRelTotal > 0 ? (100.0 * combinedRelPass / combinedRelTotal) : 0;
        double overallPct = (combinedTdsTotal + combinedRelTotal) > 0
                ? (100.0 * (combinedTdsPass + combinedRelPass) / (combinedTdsTotal + combinedRelTotal)) : 0;

        md.append("## Overall\n\n");
        md.append("Combined pass rate across function coverage and structural parity.\n\n");
        md.append("| Path | Pass | Tested | Pass Rate |\n");
        md.append("|------|------|--------|-----------|\n");
        md.append(String.format("| TDS | %d | %d | **%.1f%%** |\n", combinedTdsPass, combinedTdsTotal, overallTdsPct));
        md.append(String.format("| Relation | %d | %d | **%.1f%%** |\n", combinedRelPass, combinedRelTotal, overallRelPct));
        md.append(String.format("| **Combined** | **%d** | **%d** | **%.1f%%** |\n",
                combinedTdsPass + combinedRelPass, combinedTdsTotal + combinedRelTotal, overallPct));
        md.append("\n");

        String filePath = outputDir + "/summary.md";
        try (FileWriter fw = new FileWriter(filePath))
        {
            fw.write(md.toString());
        }
        LOGGER.info("Generated summary report: {}", filePath);
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
            // Try both "functions" (for function coverage) and "features" (for structural parity)
            JsonNode items = cat.get("functions");
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
                    String tdsState = te.has("tds") ? te.get("tds").asText() : "";
                    String relState = te.has("relation") ? te.get("relation").asText() : "";

                    if ("FAIL".equals(tdsState))
                    {
                        String anchor = "fail-" + testId + "-TDS";
                        String link = String.format("- [%s \\[TDS\\]](failure-details.md#%s)\n", testId, anchor);
                        seen.add(link);
                    }
                    if ("FAIL".equals(relState))
                    {
                        String anchor = "fail-" + testId + "-Relation";
                        String link = String.format("- [%s \\[Relation\\]](failure-details.md#%s)\n", testId, anchor);
                        seen.add(link);
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
}






