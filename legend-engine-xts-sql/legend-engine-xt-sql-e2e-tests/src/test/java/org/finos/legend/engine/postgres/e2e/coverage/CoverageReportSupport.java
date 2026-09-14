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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Shared building blocks for the catalog-driven coverage reports (function-coverage.md,
 * operator-coverage.md, and any future *-coverage.md), so every such report renders the same
 * status colouring, status-cell format, error-category classification, and "Error Details"
 * appendix, instead of each report re-implementing its own copy.
 */
public final class CoverageReportSupport
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CoverageReportSupport()
    {
    }

    public static class FailureInfo
    {
        public final String id;
        public final String path;
        public final String state;
        public final String sql;
        public final String rewrittenSql;
        public final String error;
        public final String category;

        public FailureInfo(String id, String path, String state, String sql, String rewrittenSql, String error, String category)
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

    public static class ErrorDetailEntry
    {
        public final String testId;
        public final String path;
        public final String sql;
        public final String rewrittenSql;
        public final String error;
        public final String category;

        public ErrorDetailEntry(String testId, String path, String sql, String rewrittenSql, String error, String category)
        {
            this.testId = testId;
            this.path = path;
            this.sql = sql;
            this.rewrittenSql = rewrittenSql;
            this.error = error;
            this.category = category;
        }
    }

    public static Map<String, FailureInfo> loadFailures(File parityReportFile)
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

    public static String rowColour(String tdsStatus, String relStatus)
    {
        boolean bothPass = "PASS".equals(tdsStatus) && "PASS".equals(relStatus);
        boolean bothUnsupported = "NOT_APPLICABLE".equals(tdsStatus) && "NOT_APPLICABLE".equals(relStatus);
        boolean bothError = ("ERROR".equals(tdsStatus) || "FAIL".equals(tdsStatus) || "NOT_APPLICABLE".equals(tdsStatus))
                && ("ERROR".equals(relStatus) || "FAIL".equals(relStatus) || "NOT_APPLICABLE".equals(relStatus))
                && !bothUnsupported;
        if (bothPass)
        {
            return "\uD83D\uDFE2";
        }
        if (bothUnsupported)
        {
            return "\u26AA";
        }
        if (bothError)
        {
            return "\uD83D\uDD34";
        }
        if ("PASS".equals(tdsStatus) || "PASS".equals(relStatus)
                || "PARTIAL".equals(tdsStatus) || "PARTIAL".equals(relStatus))
        {
            return "\uD83D\uDFE1";
        }
        if ("ERROR".equals(tdsStatus) || "ERROR".equals(relStatus)
                || "FAIL".equals(tdsStatus) || "FAIL".equals(relStatus))
        {
            return "\uD83D\uDD34";
        }
        return "\u26AA";
    }

    public static String statusToLabel(String status, int pass, int total)
    {
        switch (status)
        {
            case "PASS":
            case "PARTIAL":
            case "FAIL":
                return String.format("%s (%d/%d)", status, pass, total);
            case "ERROR":
                return String.format("ERROR (0/%d)", total);
            case "NOT_APPLICABLE":
                return "UNSUPPORTED";
            default:
                return "UNTESTED";
        }
    }

    public static LinkedHashSet<String> categoriesFor(List<FunctionCoverageMapper.TestResultEntry> testDetails, Map<String, FailureInfo> failureMap)
    {
        LinkedHashSet<String> categories = new LinkedHashSet<>();
        if (testDetails != null)
        {
            for (FunctionCoverageMapper.TestResultEntry te : testDetails)
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
        }
        return categories;
    }

    public static String errorCategoryLink(List<FunctionCoverageMapper.TestResultEntry> testDetails, Map<String, FailureInfo> failureMap)
    {
        LinkedHashSet<String> categories = categoriesFor(testDetails, failureMap);
        if (categories.isEmpty())
        {
            return "";
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
            String anchor = findFirstAnchorForCategory(testDetails, failureMap, category);
            if (anchor == null)
            {
                anchor = category.toLowerCase().replace("_", "-");
            }
            sb.append(String.format("[%s](#%s)", category, anchor));
        }
        return sb.toString();
    }

    private static String findFirstAnchorForCategory(List<FunctionCoverageMapper.TestResultEntry> testDetails, Map<String, FailureInfo> failureMap, String category)
    {
        if (testDetails == null)
        {
            return null;
        }
        for (FunctionCoverageMapper.TestResultEntry te : testDetails)
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

    public static void collectErrors(List<FunctionCoverageMapper.TestResultEntry> testDetails, Map<String, FailureInfo> failureMap, List<ErrorDetailEntry> allErrors)
    {
        if (testDetails == null)
        {
            return;
        }
        for (FunctionCoverageMapper.TestResultEntry te : testDetails)
        {
            FailureInfo tdsFailure = failureMap.get(te.testId + "|TDS");
            if (tdsFailure != null)
            {
                allErrors.add(new ErrorDetailEntry(te.testId, "TDS", tdsFailure.sql, tdsFailure.rewrittenSql, tdsFailure.error, tdsFailure.category));
            }
            FailureInfo relFailure = failureMap.get(te.testId + "|Relation");
            if (relFailure != null)
            {
                allErrors.add(new ErrorDetailEntry(te.testId, "Relation", relFailure.sql, relFailure.rewrittenSql, relFailure.error, relFailure.category));
            }
        }
    }

    public static void appendErrorCategoriesSection(StringBuilder md, Map<String, int[]> errorCategoryCounts)
    {
        if (errorCategoryCounts.isEmpty())
        {
            return;
        }
        md.append("## Error Categories\n\n");
        md.append("| Category | Description | TDS | Relation |\n");
        md.append("|----------|-------------|-----|----------|\n");
        for (Map.Entry<String, int[]> entry : errorCategoryCounts.entrySet())
        {
            String catAnchor = entry.getKey().toLowerCase().replace("_", "-");
            md.append(String.format("| [%s](#%s) | %s | %d | %d |\n",
                    entry.getKey(), catAnchor, ErrorCategorizer.description(entry.getKey()),
                    entry.getValue()[0], entry.getValue()[1]));
        }
        md.append("\n---\n\n");
    }

    public static void appendErrorDetailsSection(StringBuilder md, List<ErrorDetailEntry> allErrors)
    {
        if (allErrors.isEmpty())
        {
            return;
        }
        md.append("## Error Details\n\n");

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

                StringBuilder anchors = new StringBuilder();
                for (ErrorDetailEntry e : entries)
                {
                    anchors.append(String.format("<a id=\"fail-%s-%s\"></a>", e.testId, e.path));
                }

                md.append("#### ").append(anchors).append("`").append(testId).append("`\n\n");

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
}

