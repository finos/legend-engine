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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates a coverage report enumerating {@code to_char} template pattern tokens
 * and {@code EXTRACT}/{@code date_part} field keywords (PostgreSQL docs Tables 9.25
 * and 9.33), grouped by function.
 *
 * <p>Test cases opt in via {@code TestCase.format_token}, encoded as
 * {@code "<function>:<token>"} (e.g. {@code "to_char:YYYY"}, {@code "extract:isodow"})
 * so this axis can share the mutual-exclusion field validation in
 * {@link org.finos.legend.engine.postgres.e2e.TestCaseLoader} without colliding with
 * {@code function}/{@code operator}. There is no Postgres catalog for format tokens
 * (unlike functions/operators), so the token universe here is exactly what's present
 * in the YAML fixtures — this report shows test-derived coverage, not catalog-vs-test
 * coverage.
 */
public class FormatTokenCoverageReport
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FormatTokenCoverageReport.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public static class TokenEntry
    {
        public final String token;
        public String tdsState;
        public String relState;
        public final List<String> testIds = new ArrayList<>();

        public TokenEntry(String token)
        {
            this.token = token;
        }
    }

    public void generate(List<TestCaseLoader.TestCase> allTestCases, File parityReportFile, String outputDir) throws IOException
    {
        // function -> token -> entry
        Map<String, Map<String, TokenEntry>> byFunction = new LinkedHashMap<>();
        Map<String, TokenEntry> byTestId = new HashMap<>();

        for (TestCaseLoader.TestCase tc : allTestCases)
        {
            if (tc.format_token == null)
            {
                continue;
            }
            int idx = tc.format_token.indexOf(':');
            String function = idx > 0 ? tc.format_token.substring(0, idx) : "unknown";
            String token = idx > 0 ? tc.format_token.substring(idx + 1) : tc.format_token;

            TokenEntry entry = byFunction
                    .computeIfAbsent(function, k -> new LinkedHashMap<>())
                    .computeIfAbsent(token, TokenEntry::new);
            entry.testIds.add(tc.id);
            byTestId.put(tc.id, entry);
        }

        if (byFunction.isEmpty())
        {
            LOGGER.info("No format_token test cases found, skipping format token coverage report");
            return;
        }

        if (parityReportFile != null && parityReportFile.exists())
        {
            JsonNode report = MAPPER.readTree(parityReportFile);
            JsonNode results = report.get("results");
            if (results != null)
            {
                for (JsonNode result : results)
                {
                    String id = result.has("id") ? result.get("id").asText() : "";
                    String state = result.has("state") ? result.get("state").asText() : "";
                    String path = result.has("path") ? result.get("path").asText() : "";
                    TokenEntry entry = byTestId.get(id);
                    if (entry == null)
                    {
                        continue;
                    }
                    if ("TDS".equals(path) && entry.tdsState == null)
                    {
                        entry.tdsState = state;
                    }
                    else if ("Relation".equals(path) && entry.relState == null)
                    {
                        entry.relState = state;
                    }
                }
            }
        }

        new File(outputDir).mkdirs();
        generateJson(byFunction, outputDir + "/format-token-coverage.json");
        generateMarkdown(byFunction, outputDir + "/format-token-coverage.md");
        printConsoleSummary(byFunction);
    }

    private void generateJson(Map<String, Map<String, TokenEntry>> byFunction, String path) throws IOException
    {
        ObjectNode root = MAPPER.createObjectNode();
        int total = 0;
        int tdsPass = 0;
        int relPass = 0;
        for (Map<String, TokenEntry> tokens : byFunction.values())
        {
            for (TokenEntry e : tokens.values())
            {
                total++;
                if ("PASS".equals(e.tdsState))
                {
                    tdsPass++;
                }
                if ("PASS".equals(e.relState))
                {
                    relPass++;
                }
            }
        }
        ObjectNode summary = root.putObject("summary");
        summary.put("total_tokens", total);
        summary.put("tds_pass", tdsPass);
        summary.put("relation_pass", relPass);

        ArrayNode functions = root.putArray("functions");
        for (Map.Entry<String, Map<String, TokenEntry>> fEntry : byFunction.entrySet())
        {
            ObjectNode fNode = functions.addObject();
            fNode.put("function", fEntry.getKey());
            ArrayNode tokens = fNode.putArray("tokens");
            for (TokenEntry e : fEntry.getValue().values())
            {
                ObjectNode tNode = tokens.addObject();
                tNode.put("token", e.token);
                tNode.put("tds_status", e.tdsState == null ? "UNTESTED" : e.tdsState);
                tNode.put("relation_status", e.relState == null ? "UNTESTED" : e.relState);
                ArrayNode ids = tNode.putArray("test_ids");
                for (String id : e.testIds)
                {
                    ids.add(id);
                }
            }
        }

        MAPPER.writeValue(new File(path), root);
    }

    private void generateMarkdown(Map<String, Map<String, TokenEntry>> byFunction, String path) throws IOException
    {
        StringBuilder md = new StringBuilder();
        md.append("# Postgres Date/Time Format Token Coverage — Legend SQL\n\n");
        md.append("Reference: [PostgreSQL 16 Data Type Formatting Functions]"
                + "(https://www.postgresql.org/docs/16/functions-formatting.html) (Table 9.25) and "
                + "[Date/Time Functions](https://www.postgresql.org/docs/16/functions-datetime.html) (Table 9.33).\n\n");

        for (Map.Entry<String, Map<String, TokenEntry>> fEntry : byFunction.entrySet())
        {
            String function = fEntry.getKey();
            Map<String, TokenEntry> tokens = fEntry.getValue();
            int tdsPass = 0;
            int relPass = 0;
            for (TokenEntry e : tokens.values())
            {
                if ("PASS".equals(e.tdsState))
                {
                    tdsPass++;
                }
                if ("PASS".equals(e.relState))
                {
                    relPass++;
                }
            }

            md.append("## `").append(function).append("` tokens (")
                    .append(tdsPass).append("/").append(tokens.size()).append(" TDS PASS, ")
                    .append(relPass).append("/").append(tokens.size()).append(" Relation PASS)\n\n");
            md.append("| Token | TDS | Relation | Test(s) |\n");
            md.append("|---|---|---|---|\n");
            for (TokenEntry e : tokens.values())
            {
                md.append("| `").append(e.token).append("` | ")
                        .append(e.tdsState == null ? "UNTESTED" : e.tdsState).append(" | ")
                        .append(e.relState == null ? "UNTESTED" : e.relState).append(" | ")
                        .append(String.join(", ", e.testIds)).append(" |\n");
            }
            md.append("\n");
        }

        try (FileWriter writer = new FileWriter(path))
        {
            writer.write(md.toString());
        }
    }

    private void printConsoleSummary(Map<String, Map<String, TokenEntry>> byFunction)
    {
        int total = 0;
        int tdsPass = 0;
        for (Map<String, TokenEntry> tokens : byFunction.values())
        {
            total += tokens.size();
            for (TokenEntry e : tokens.values())
            {
                if ("PASS".equals(e.tdsState))
                {
                    tdsPass++;
                }
            }
        }
        LOGGER.info("Format token coverage: {}/{} tokens PASS on TDS path", tdsPass, total);
    }
}

