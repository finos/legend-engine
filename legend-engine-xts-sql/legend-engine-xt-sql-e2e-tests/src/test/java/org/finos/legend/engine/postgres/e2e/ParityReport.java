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

package org.finos.legend.engine.postgres.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects test results and produces JSON + console report.
 */
public class ParityReport
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ParityReport.class);

    private final List<TestResult> results = Collections.synchronizedList(new ArrayList<>());

    public void record(TestResult result)
    {
        results.add(result);
    }

    public void printSummary()
    {
        Map<String, CategorySummary> tdsCategories = new HashMap<>();
        Map<String, CategorySummary> relCategories = new HashMap<>();
        Counters tdsOverall = new Counters();
        Counters relOverall = new Counters();

        for (TestResult r : results)
        {
            if ("TDS".equals(r.path))
            {
                tdsCategories.computeIfAbsent(r.category, k -> new CategorySummary(k)).add(r.state);
                tdsOverall.add(r.state);
            }
            else
            {
                relCategories.computeIfAbsent(r.category, k -> new CategorySummary(k)).add(r.state);
                relOverall.add(r.state);
            }
        }

        LOGGER.info("\n═══════════════════════════════════════════════════════════════════════════");
        LOGGER.info("POSTGRES PARITY TEST RESULTS");
        LOGGER.info("═══════════════════════════════════════════════════════════════════════════\n");

        for (String cat : tdsCategories.keySet())
        {
            CategorySummary tds = tdsCategories.get(cat);
            CategorySummary rel = relCategories.getOrDefault(cat, new CategorySummary(cat));
            LOGGER.info("Category: " + cat);
            LOGGER.info("  TDS path:      " + tds.counters);
            LOGGER.info("  Relation path: " + rel.counters);
            LOGGER.info("");
        }

        LOGGER.info("═══════════════════════════════════════════════════════════════════════════");
        LOGGER.info("OVERALL (TDS):      " + tdsOverall);
        LOGGER.info("OVERALL (Relation): " + relOverall);
        LOGGER.info("═══════════════════════════════════════════════════════════════════════════\n");

        // Print failures
        long failCount = results.stream().filter(r -> !"PASS".equals(r.state) && !"SKIP".equals(r.state)).count();
        if (failCount > 0)
        {
            LOGGER.info("FAILURES (" + failCount + "):");
            LOGGER.info("───────────────────────────────────────────────────────────────────────────");
            for (TestResult r : results)
            {
                if (!"PASS".equals(r.state) && !"SKIP".equals(r.state))
                {
                    LOGGER.info("  [" + r.state + "] " + r.id + " (" + r.path + ")");
                    LOGGER.info("    SQL: " + r.sql);
                    if (r.rewrittenSql != null)
                    {
                        LOGGER.info("    Rewritten: " + r.rewrittenSql);
                    }
                    if (r.error != null)
                    {
                        LOGGER.info("    Error: " + r.error);
                    }
                    if (r.diffs != null && !r.diffs.isEmpty())
                    {
                        for (String diff : r.diffs)
                        {
                            LOGGER.info("    Diff: " + diff);
                        }
                    }
                    LOGGER.info("");
                }
            }
        }
    }

    public void writeJsonReport(File outputFile) throws IOException
    {
        Map<String, Object> report = new HashMap<>();

        // Summary
        Map<String, Counters> summaryMap = new HashMap<>();
        summaryMap.put("tds", new Counters());
        summaryMap.put("relation", new Counters());

        Map<String, Map<String, Counters>> categoryMap = new HashMap<>();

        for (TestResult r : results)
        {
            String pathKey = "TDS".equals(r.path) ? "tds" : "relation";
            summaryMap.get(pathKey).add(r.state);
            categoryMap.computeIfAbsent(r.category, k ->
            {
                Map<String, Counters> m = new HashMap<>();
                m.put("tds", new Counters());
                m.put("relation", new Counters());
                return m;
            }).get(pathKey).add(r.state);
        }

        report.put("summary", summaryMap);

        List<Map<String, Object>> categories = new ArrayList<>();
        for (Map.Entry<String, Map<String, Counters>> entry : categoryMap.entrySet())
        {
            Map<String, Object> cat = new HashMap<>();
            cat.put("name", entry.getKey());
            cat.put("tds", entry.getValue().get("tds"));
            cat.put("relation", entry.getValue().get("relation"));
            categories.add(cat);
        }
        report.put("categories", categories);

        // Failures
        List<TestResult> failures = new ArrayList<>();
        for (TestResult r : results)
        {
            if (!"PASS".equals(r.state) && !"SKIP".equals(r.state))
            {
                failures.add(r);
            }
        }
        report.put("failures", failures);

        // All results (for function coverage mapping)
        List<Map<String, String>> allResults = new ArrayList<>();
        for (TestResult r : results)
        {
            Map<String, String> entry = new HashMap<>();
            entry.put("id", r.id);
            entry.put("path", r.path);
            entry.put("state", r.state);
            allResults.add(entry);
        }
        report.put("results", allResults);

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        outputFile.getParentFile().mkdirs();
        mapper.writeValue(outputFile, report);
    }

    public boolean hasFailures()
    {
        return results.stream().anyMatch(r -> "FAIL".equals(r.state) || "ERROR".equals(r.state));
    }

    public List<TestResult> getResults()
    {
        return results;
    }

    public static class TestResult
    {
        public String id;
        public String category;
        public String path;    // "TDS" or "Relation"
        public String state;   // "PASS", "FAIL", "ERROR", "SKIP", "BUG"
        public String sql;
        public String rewrittenSql;
        public String error;
        public List<String> diffs;
        public transient ResultMatrix expectedResult;
        public transient ResultMatrix actualResult;

        public TestResult(String id, String category, String path, String state, String sql, String rewrittenSql, String error, List<String> diffs)
        {
            this(id, category, path, state, sql, rewrittenSql, error, diffs, null, null);
        }

        public TestResult(String id, String category, String path, String state, String sql,
                          String rewrittenSql, String error, List<String> diffs,
                          ResultMatrix expectedResult, ResultMatrix actualResult)
        {
            this.id = id;
            this.category = category;
            this.path = path;
            this.state = state;
            this.sql = sql;
            this.rewrittenSql = rewrittenSql;
            this.error = simplifyError(error);
            this.diffs = diffs;
            this.expectedResult = expectedResult;
            this.actualResult = actualResult;
        }

        /**
         * Extracts the meaningful error message from verbose Legend SQL server error responses.
         * <p>
         * Typical noisy format:
         * {@code ERROR: java.lang.RuntimeException: Legend SQL API returned 500: {"status":"error",...,
         * "message":"PureAssertFailException: Assert failure at (resource:... line:XX column:YY),
         * \"actual error\"","trace":"<huge stack trace>"}}
         * <p>
         * This method extracts just the inner quoted message (e.g., {@code No function matches the given name "ACOSH"}).
         */
        private static String simplifyError(String error)
        {
            if (error == null)
            {
                return null;
            }

            // Strategy 1: Find the "message" field in embedded JSON and extract the meaningful part.
            // The error string contains a JSON body with: "message":"<exception message>","trace":"..."
            int msgIdx = error.indexOf("\"message\"");
            if (msgIdx >= 0)
            {
                // Find the colon and opening quote after "message"
                int colonIdx = error.indexOf(':', msgIdx + 9);
                if (colonIdx >= 0)
                {
                    int openQuote = error.indexOf('"', colonIdx + 1);
                    if (openQuote >= 0)
                    {
                        // Find the closing quote — must handle escaped quotes (\" in the string)
                        String msgContent = extractJsonStringValue(error, openQuote);
                        if (msgContent != null)
                        {
                            // Strip "PureAssertFailException: Assert failure at (...), " prefix
                            int assertComma = msgContent.indexOf("), ");
                            if (assertComma >= 0 && msgContent.contains("Assert failure at"))
                            {
                                String inner = msgContent.substring(assertComma + 3);
                                // Remove wrapping escaped quotes if present
                                if (inner.startsWith("\\\""))
                                {
                                    inner = inner.substring(2);
                                }
                                if (inner.endsWith("\\\""))
                                {
                                    inner = inner.substring(0, inner.length() - 2);
                                }
                                // Unescape remaining
                                inner = inner.replace("\\\"", "\"");
                                return inner;
                            }
                            // Strip exception class prefix
                            msgContent = msgContent.replaceFirst("^\\w+Exception:\\s*", "");
                            msgContent = msgContent.replace("\\\"", "\"");
                            return msgContent;
                        }
                    }
                }
            }

            // Strategy 2: Strip "Where: org.finos..." stack trace suffix
            int whereIdx = error.indexOf("\n  Where:");
            if (whereIdx < 0)
            {
                whereIdx = error.indexOf("\nWhere:");
            }
            if (whereIdx > 0)
            {
                return error.substring(0, whereIdx).trim();
            }

            // Strategy 3: Strip stack traces (lines starting with \tat)
            int stackIdx = error.indexOf("\n\tat ");
            if (stackIdx < 0)
            {
                stackIdx = error.indexOf("\\n\\tat ");
            }
            if (stackIdx > 0)
            {
                return error.substring(0, stackIdx).trim();
            }

            return error;
        }

        /**
         * Extracts a JSON string value starting at the opening quote position,
         * correctly handling escaped quotes (backslash-quote) within the value.
         */
        private static String extractJsonStringValue(String s, int openQuoteIdx)
        {
            int start = openQuoteIdx + 1;
            StringBuilder sb = new StringBuilder();
            for (int i = start; i < s.length(); i++)
            {
                char c = s.charAt(i);
                if (c == '\\' && i + 1 < s.length())
                {
                    char next = s.charAt(i + 1);
                    if (next == '"' || next == '\\' || next == 'n' || next == 't' || next == 'r')
                    {
                        sb.append(c);
                        sb.append(next);
                        i++;
                        continue;
                    }
                }
                if (c == '"')
                {
                    // Found closing quote
                    return sb.toString();
                }
                sb.append(c);
            }
            // No closing quote found — return what we have
            return sb.length() > 0 ? sb.toString() : null;
        }
    }

    private static class CategorySummary
    {
        String name;
        Counters counters = new Counters();

        CategorySummary(String name)
        {
            this.name = name;
        }

        void add(String state)
        {
            counters.add(state);
        }
    }

    public static class Counters
    {
        public int pass = 0;
        public int fail = 0;
        public int error = 0;
        public int skip = 0;
        public int bug = 0;
        public int total = 0;

        void add(String state)
        {
            total++;
            switch (state)
            {
                case "PASS":
                    pass++;
                    break;
                case "FAIL":
                    fail++;
                    break;
                case "ERROR":
                    error++;
                    break;
                case "SKIP":
                    skip++;
                    break;
                case "BUG":
                    bug++;
                    break;
            }
        }

        @Override
        public String toString()
        {
            return "PASS: " + pass + " | FAIL: " + fail + " | ERROR: " + error + " | SKIP: " + skip + " | BUG: " + bug + " | TOTAL: " + total;
        }
    }
}

