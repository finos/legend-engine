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

import org.finos.legend.engine.postgres.e2e.ParityReport;
import org.finos.legend.engine.postgres.e2e.ResultMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates a detailed failure report (failure-details.md) showing full result set
 * comparisons between Postgres and Legend SQL for every FAIL test.
 *
 * Each entry includes:
 * - The original SQL and rewritten SQL
 * - For FAIL: full expected (Postgres) and actual (Legend) result sets side by side
 *
 * Other reports (function-coverage.md, structural-parity.md) link directly to
 * anchors in this file using the pattern: fail-{testId}-{path}
 */
public class FailureDetailReport
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FailureDetailReport.class);

    public void generate(List<ParityReport.TestResult> results, String outputDir)
    {
        String path = outputDir + "/failure-details.md";
        StringBuilder md = new StringBuilder();

        md.append("# Failure Details — Result Set Comparisons\n\n");
        md.append("Full expected (Postgres) vs actual (Legend) result sets for every test where results differ.\n\n");
        md.append("---\n\n");

        // Only FAIL tests (result mismatches)
        List<ParityReport.TestResult> fails = results.stream()
                .filter(r -> "FAIL".equals(r.state))
                .collect(Collectors.toList());

        md.append(String.format("**Total result mismatches:** %d\n\n", fails.size()));

        if (fails.isEmpty())
        {
            md.append("_No result mismatches — all executed queries match Postgres._\n");
            writeFile(path, md.toString());
            LOGGER.info("Failure detail report: {} (0 FAILs)", path);
            return;
        }

        // TOC
        md.append("## Table of Contents\n\n");
        for (ParityReport.TestResult r : fails)
        {
            String anchor = "fail-" + r.id + "-" + r.path;
            md.append(String.format("- [%s \\[%s\\]](#%s)\n", r.id, r.path, anchor));
        }
        md.append("\n---\n\n");

        // Detail sections
        for (ParityReport.TestResult r : fails)
        {
            String anchor = "fail-" + r.id + "-" + r.path;
            md.append(String.format("<a id=\"%s\"></a>\n\n", anchor));
            md.append(String.format("### ❌ %s [%s]\n\n", r.id, r.path));

            md.append("**SQL (Postgres):**\n```sql\n").append(r.sql).append("\n```\n\n");
            if (r.rewrittenSql != null && !r.rewrittenSql.equals(r.sql))
            {
                md.append("**SQL (Legend, rewritten):**\n```sql\n").append(r.rewrittenSql).append("\n```\n\n");
            }

            // Diffs summary
            if (r.diffs != null && !r.diffs.isEmpty())
            {
                md.append("**Differences:**\n");
                for (String diff : r.diffs)
                {
                    md.append("- ").append(escapeMarkdown(diff)).append("\n");
                }
                md.append("\n");
            }

            // Full result sets
            if (r.expectedResult != null)
            {
                md.append("**Expected (Postgres):**\n\n");
                appendResultTable(md, r.expectedResult);
                md.append("\n");
            }
            else
            {
                md.append("**Expected (Postgres):** _(result set not captured)_\n\n");
            }

            if (r.actualResult != null)
            {
                md.append("**Actual (Legend):**\n\n");
                appendResultTable(md, r.actualResult);
                md.append("\n");
            }
            else
            {
                md.append("**Actual (Legend):** _(result set not captured)_\n\n");
            }

            md.append("---\n\n");
        }

        writeFile(path, md.toString());
        LOGGER.info("Failure detail report: {} ({} FAILs)", path, fails.size());
    }

    private void writeFile(String path, String content)
    {
        try (FileWriter fw = new FileWriter(path))
        {
            fw.write(content);
        }
        catch (IOException e)
        {
            LOGGER.error("Failed to write failure detail report: {}", path, e);
        }
    }

    private void appendResultTable(StringBuilder md, ResultMatrix matrix)
    {
        List<String> cols = matrix.getColumnNames();
        if (cols.isEmpty())
        {
            md.append("_(empty result set)_\n");
            return;
        }
        // Header
        md.append("| ");
        md.append(cols.stream().map(FailureDetailReport::escapeMarkdown).collect(Collectors.joining(" | ")));
        md.append(" |\n");
        // Separator
        md.append("| ");
        md.append(cols.stream().map(c -> "---").collect(Collectors.joining(" | ")));
        md.append(" |\n");
        // Rows (cap at 50 to keep report manageable)
        int rowLimit = Math.min(matrix.getRowCount(), 50);
        for (int i = 0; i < rowLimit; i++)
        {
            List<Object> row = matrix.getRows().get(i);
            md.append("| ");
            md.append(row.stream()
                    .map(v -> v == null ? "_NULL_" : escapeMarkdown(v.toString()))
                    .collect(Collectors.joining(" | ")));
            md.append(" |\n");
        }
        if (matrix.getRowCount() > rowLimit)
        {
            md.append(String.format("| _... %d more rows ..._ ", matrix.getRowCount() - rowLimit));
            for (int i = 1; i < cols.size(); i++)
            {
                md.append("| ");
            }
            md.append("|\n");
        }
    }

    private static String escapeMarkdown(String s)
    {
        if (s == null)
        {
            return "";
        }
        return s.replace("|", "\\|").replace("\n", " ").replace("\r", "");
    }
}
