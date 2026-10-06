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

package org.finos.legend.engine.postgres.e2e.coverage.site;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.postgres.e2e.ParityReport;
import org.finos.legend.engine.postgres.e2e.ResultMatrix;
import org.finos.legend.engine.postgres.e2e.TestCaseLoader;
import org.finos.legend.engine.postgres.e2e.coverage.DocLinks;
import org.finos.legend.engine.postgres.e2e.coverage.FormatTokenCoverageReport;
import org.finos.legend.engine.postgres.e2e.coverage.FunctionCatalogExtractor;
import org.finos.legend.engine.postgres.e2e.coverage.FunctionCoverageMapper;
import org.finos.legend.engine.postgres.e2e.coverage.OperatorCatalogExtractor;
import org.finos.legend.engine.postgres.e2e.coverage.OperatorCoverageMapper;
import org.finos.legend.engine.postgres.e2e.coverage.StructuralParityReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders the unified static parity site described in {@code docs/parity-site-plan.md}.
 *
 * <p>This is a <em>view</em> over data already produced by the existing coverage mappers
 * ({@link FunctionCoverageMapper}, {@link OperatorCoverageMapper}, {@link StructuralParityReport},
 * {@link FormatTokenCoverageReport}) ? no status/coverage logic is re-derived here (plan &sect;10.4).
 *
 * <p>Simplification vs. the full plan: operator pages render as flat signature tables (like the
 * function pages) rather than a left-type/right-type matrix, and there is no client-side TDS/Relation
 * path toggle ? both paths are always shown as adjacent columns. Everything else in the plan's
 * information architecture (areas, per-test permalinks, filter box, show-untested toggle, doc links)
 * is implemented.
 */
public class DocumentationSiteGenerator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentationSiteGenerator.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String outputDir;
    private String legendRevision;
    private TestIndex testIndex;
    private Map<String, TestCaseLoader.TestCase> testCasesById;
    private List<NavEntry> nav = new ArrayList<>();

    public void generate(
            Map<String, List<FunctionCatalogExtractor.PgFunction>> functionCatalog,
            Map<String, List<OperatorCatalogExtractor.PgOperator>> operatorCatalog,
            List<TestCaseLoader.TestCase> allTestCases,
            File parityReportJson,
            List<ParityReport.TestResult> inMemoryResults,
            String legendRevision,
            String outputDir) throws IOException
    {
        this.outputDir = outputDir;
        this.legendRevision = legendRevision;
        this.testCasesById = new LinkedHashMap<>();
        for (TestCaseLoader.TestCase tc : allTestCases)
        {
            testCasesById.put(tc.id, tc);
        }
        this.testIndex = TestIndex.build(parityReportJson, inMemoryResults, MAPPER);

        new File(outputDir).mkdirs();
        new File(outputDir, "functions").mkdirs();
        new File(outputDir, "operators").mkdirs();
        new File(outputDir, "structural").mkdirs();
        new File(outputDir, "compositions").mkdirs();
        new File(outputDir, "test").mkdirs();
        new File(outputDir, "test/group").mkdirs();
        writeAssets();

        FunctionCoverageMapper functionMapper = new FunctionCoverageMapper();
        if (functionCatalog != null)
        {
            functionMapper.mapCoverage(functionCatalog, parityReportJson, allTestCases);
        }
        OperatorCoverageMapper operatorMapper = new OperatorCoverageMapper();
        if (operatorCatalog != null)
        {
            operatorMapper.mapCoverage(operatorCatalog, parityReportJson, allTestCases);
        }
        StructuralParityReport structuralReport = new StructuralParityReport();
        Map<String, Map<String, StructuralParityReport.FeatureCoverage>> structuralCategories =
                structuralReport.buildCategories(allTestCases, parityReportJson);
        FormatTokenCoverageReport tokenReport = new FormatTokenCoverageReport();
        Map<String, Map<String, FormatTokenCoverageReport.TokenEntry>> tokenMap =
                tokenReport.buildTokenMap(allTestCases, parityReportJson);

        List<AreaSummary> areaSummaries = new ArrayList<>();

        if (functionCatalog != null)
        {
            AreaSummary functionsArea = new AreaSummary("Functions", "functions");
            for (Map.Entry<String, List<FunctionCatalogExtractor.PgFunction>> e : functionCatalog.entrySet())
            {
                String category = e.getKey();
                List<FunctionCatalogExtractor.PgFunction> fns = e.getValue();
                if (fns.isEmpty())
                {
                    continue;
                }
                String slug = DocLinks.slugFor(category);
                CategorySummary summary = renderFunctionCategory(category, slug, fns);
                functionsArea.categories.add(summary);
                nav.add(new NavEntry("Functions", category, "functions/" + slug + ".html"));
            }
            areaSummaries.add(functionsArea);
        }

        if (operatorCatalog != null)
        {
            AreaSummary operatorsArea = new AreaSummary("Operators", "operators");
            for (Map.Entry<String, List<OperatorCatalogExtractor.PgOperator>> e : operatorCatalog.entrySet())
            {
                String category = e.getKey();
                List<OperatorCatalogExtractor.PgOperator> ops = e.getValue();
                if (ops.isEmpty())
                {
                    continue;
                }
                String slug = DocLinks.slugFor(category);
                CategorySummary summary = renderOperatorCategory(category, slug, ops);
                operatorsArea.categories.add(summary);
                nav.add(new NavEntry("Operators", category, "operators/" + slug + ".html"));
            }
            areaSummaries.add(operatorsArea);
        }

        AreaSummary structuralArea = new AreaSummary("Structural", "structural");
        AreaSummary compositionsArea = new AreaSummary("Compositions", "compositions");
        for (Map.Entry<String, Map<String, StructuralParityReport.FeatureCoverage>> e : structuralCategories.entrySet())
        {
            String category = e.getKey();
            Map<String, StructuralParityReport.FeatureCoverage> features = e.getValue();
            boolean isComposition = "compositions".equalsIgnoreCase(category);
            String dir = isComposition ? "compositions" : "structural";
            String slug = DocLinks.slugFor(category);
            CategorySummary summary = renderStructuralCategory(category, slug, dir, features);
            (isComposition ? compositionsArea : structuralArea).categories.add(summary);
            nav.add(new NavEntry(isComposition ? "Compositions" : "Structural", category, dir + "/" + slug + ".html"));
        }
        if (!structuralArea.categories.isEmpty())
        {
            areaSummaries.add(structuralArea);
        }
        if (!compositionsArea.categories.isEmpty())
        {
            areaSummaries.add(compositionsArea);
        }

        if (!tokenMap.isEmpty())
        {
            AreaSummary tokensArea = new AreaSummary("Format Tokens", "format-tokens");
            CategorySummary tokenSummary = renderFormatTokens(tokenMap);
            tokensArea.categories.add(tokenSummary);
            nav.add(new NavEntry("Format Tokens", "Format Tokens", "format-tokens.html"));
            areaSummaries.add(tokensArea);
        }

        for (TestCaseLoader.TestCase tc : allTestCases)
        {
            renderTestPage(tc);
        }

        renderOverview(areaSummaries);
        renderMasterDetailIndex(areaSummaries);
        LOGGER.info("Parity site generated at {}", new File(outputDir).getAbsolutePath());
    }

    // ===================== Functions =====================

    private CategorySummary renderFunctionCategory(String category, String slug, List<FunctionCatalogExtractor.PgFunction> fns) throws IOException
    {
        CategorySummary summary = new CategorySummary(category, "functions/" + slug + ".html");
        StringBuilder rows = new StringBuilder();
        for (FunctionCatalogExtractor.PgFunction fn : fns)
        {
            boolean untested = "UNTESTED".equals(fn.tdsStatus) && "UNTESTED".equals(fn.relStatus);
            summary.total++;
            if (!untested)
            {
                summary.tested++;
            }
            if ("PASS".equals(fn.tdsStatus))
            {
                summary.tdsPass++;
            }
            if ("PASS".equals(fn.relStatus))
            {
                summary.relPass++;
            }
            if (isUnsupported(fn.tdsStatus))
            {
                summary.tdsUnsupported++;
            }
            if (isUnsupported(fn.relStatus))
            {
                summary.relUnsupported++;
            }
            int total = fn.coverage != null ? fn.coverage.total : 0;
            int pass = fn.coverage != null ? fn.coverage.tdsPass : 0;
            String href = groupHref(slug + "__" + DocLinks.slugFor(fn.signature), fn.signature, testEntries(fn.coverage));
            rows.append(entryRow(fn.name, fn.signature, fn.tdsStatus, fn.relStatus, pass, total, untested, href));
        }
        String body = categoryPageBody(category, "Function", rows.toString());
        writePage(outputDir + "/functions/" + slug + ".html", pageShell(category + " ? Functions", body, 1));
        return summary;
    }

    // ===================== Operators =====================

    private CategorySummary renderOperatorCategory(String category, String slug, List<OperatorCatalogExtractor.PgOperator> ops) throws IOException
    {
        CategorySummary summary = new CategorySummary(category, "operators/" + slug + ".html");
        StringBuilder rows = new StringBuilder();
        for (OperatorCatalogExtractor.PgOperator op : ops)
        {
            boolean untested = "UNTESTED".equals(op.tdsStatus) && "UNTESTED".equals(op.relStatus);
            summary.total++;
            if (!untested)
            {
                summary.tested++;
            }
            if ("PASS".equals(op.tdsStatus))
            {
                summary.tdsPass++;
            }
            if ("PASS".equals(op.relStatus))
            {
                summary.relPass++;
            }
            if (isUnsupported(op.tdsStatus))
            {
                summary.tdsUnsupported++;
            }
            if (isUnsupported(op.relStatus))
            {
                summary.relUnsupported++;
            }
            int total = op.coverage != null ? op.coverage.total : 0;
            int pass = op.coverage != null ? op.coverage.tdsPass : 0;
            String sig = op.leftType + " " + op.name + " " + op.rightType + " \u2192 " + op.resultType;
            String href = groupHref(slug + "__" + DocLinks.slugFor(sig), sig, testEntries(op.coverage));
            rows.append(entryRow(op.name, sig, op.tdsStatus, op.relStatus, pass, total, untested, href));
        }
        String body = categoryPageBody(category, "Operator", rows.toString());
        writePage(outputDir + "/operators/" + slug + ".html", pageShell(category + " ? Operators", body, 1));
        return summary;
    }

    // ===================== Structural / Compositions =====================

    private CategorySummary renderStructuralCategory(String category, String slug, String dir, Map<String, StructuralParityReport.FeatureCoverage> features) throws IOException
    {
        CategorySummary summary = new CategorySummary(category, dir + "/" + slug + ".html");
        StringBuilder rows = new StringBuilder();
        for (StructuralParityReport.FeatureCoverage fc : features.values())
        {
            String tdsStatus = fc.tdsStatus();
            String relStatus = fc.relStatus();
            boolean untested = fc.total == 0;
            summary.total++;
            if (!untested)
            {
                summary.tested++;
            }
            if ("PASS".equals(tdsStatus))
            {
                summary.tdsPass++;
            }
            if ("PASS".equals(relStatus))
            {
                summary.relPass++;
            }
            if (isUnsupported(tdsStatus))
            {
                summary.tdsUnsupported++;
            }
            if (isUnsupported(relStatus))
            {
                summary.relUnsupported++;
            }
            List<TestLinkEntry> entries = new ArrayList<>();
            for (StructuralParityReport.TestEntry te : fc.tests)
            {
                entries.add(new TestLinkEntry(te.testId, te.tdsState, te.relationState));
            }
            String href = groupHref(slug + "__" + DocLinks.slugFor(fc.featureName), fc.featureName, entries);
            rows.append(entryRow(fc.featureName, fc.categoryName, tdsStatus, relStatus, fc.tdsPass, fc.total, untested, href));
        }
        String body = categoryPageBody(category, "Feature", rows.toString());
        writePage(outputDir + "/" + dir + "/" + slug + ".html", pageShell(category, body, 1));
        return summary;
    }

    // ===================== Format tokens =====================

    private CategorySummary renderFormatTokens(Map<String, Map<String, FormatTokenCoverageReport.TokenEntry>> tokenMap) throws IOException
    {
        CategorySummary summary = new CategorySummary("Format Tokens", "format-tokens.html");
        StringBuilder body = new StringBuilder();
        body.append("<h1>Format Tokens</h1>");
        body.append("<p class=\"pg-doc-link\"><a href=\"https://www.postgresql.org/docs/16/functions-formatting.html\">PostgreSQL docs: Data Type Formatting Functions &#8599;</a></p>");
        for (Map.Entry<String, Map<String, FormatTokenCoverageReport.TokenEntry>> fEntry : tokenMap.entrySet())
        {
            body.append("<h2><code>").append(Html.escape(fEntry.getKey())).append("</code> tokens</h2>");
            StringBuilder rows = new StringBuilder();
            for (FormatTokenCoverageReport.TokenEntry te : fEntry.getValue().values())
            {
                summary.total++;
                boolean untested = te.tdsState == null && te.relState == null;
                if (!untested)
                {
                    summary.tested++;
                }
                if ("PASS".equals(te.tdsState))
                {
                    summary.tdsPass++;
                }
                if ("PASS".equals(te.relState))
                {
                    summary.relPass++;
                }
                if (isUnsupported(te.tdsState))
                {
                    summary.tdsUnsupported++;
                }
                if (isUnsupported(te.relState))
                {
                    summary.relUnsupported++;
                }
                List<TestLinkEntry> entries = new ArrayList<>();
                for (String id : te.testIds)
                {
                    entries.add(new TestLinkEntry(id, te.tdsState, te.relState));
                }
                String href = groupHref("token__" + DocLinks.slugFor(fEntry.getKey()) + "__" + DocLinks.slugFor(te.token), te.token, entries);
                rows.append(entryRow(te.token, fEntry.getKey(), te.tdsState, te.relState, 0, te.testIds.size(), untested, href));
            }
            body.append(tableShell("Token", rows.toString()));
        }
        writePage(outputDir + "/format-tokens.html", pageShell("Format Tokens", body.toString(), 1));
        return summary;
    }

    // ===================== Test detail pages =====================

    private void renderTestPage(TestCaseLoader.TestCase tc) throws IOException
    {
        TestIndex.TestPathInfo tds = testIndex.get(tc.id, "TDS");
        TestIndex.TestPathInfo rel = testIndex.get(tc.id, "Relation");

        StringBuilder body = new StringBuilder();
        body.append("<h1>").append(Html.escape(tc.id)).append("</h1>");
        body.append("<table class=\"kv\">");
        appendKv(body, "Category", tc.category != null ? tc.category : "-");
        appendKv(body, "Feature", tc.feature != null ? tc.feature : "-");
        appendKv(body, "Function", tc.function != null ? tc.function : "-");
        appendKv(body, "Operator", tc.operator != null ? tc.operator : "-");
        appendKv(body, "Format token", tc.format_token != null ? tc.format_token : "-");
        appendKv(body, "Signature", tc.signature != null ? tc.signature : "-");
        body.append("</table>");

        body.append("<table class=\"status-table\"><tr><th>Path</th><th>Status</th><th>Expected</th></tr>");
        body.append(statusRow("TDS", tds, tc.expected_tds_status));
        body.append(statusRow("Relation", rel, tc.expected_rel_status));
        body.append("</table>");

        body.append("<h2>Reference SQL</h2><pre class=\"sql\">").append(Html.escape(tc.sql)).append("</pre>");
        appendPathDetail(body, "TDS", tds);
        appendPathDetail(body, "Relation", rel);

        writePage(outputDir + "/test/" + tc.id + ".html", pageShell(tc.id, body.toString(), 2));
    }

    private String statusRow(String path, TestIndex.TestPathInfo info, String expected)
    {
        String state = info != null ? info.state : null;
        StatusGlyph.Glyph g = StatusGlyph.forStatus(state);
        return "<tr><td>" + path + "</td><td class=\"" + g.cssClass + "\" aria-label=\"" + g.ariaLabel + "\">"
                + g.symbol + " " + (state == null ? "UNTESTED" : state) + "</td><td>" + (expected == null ? "-" : Html.escape(expected)) + "</td></tr>";
    }

    private void appendPathDetail(StringBuilder body, String path, TestIndex.TestPathInfo info)
    {
        if (info == null)
        {
            return;
        }
        String state = info.state;
        StatusGlyph.Glyph g = StatusGlyph.forStatus(state);
        body.append("<h2 id=\"").append(path.toLowerCase()).append("\">").append(path).append(" path &mdash; ")
                .append("<span class=\"").append(g.cssClass).append("\">").append(g.symbol).append(" ")
                .append(state == null ? "UNTESTED" : state).append("</span></h2>");

        // 1. The SQL that ran, for every test regardless of outcome.
        if (info.sql != null && !info.sql.isEmpty())
        {
            body.append("<h3>Input SQL</h3><pre class=\"sql\">").append(Html.escape(info.sql)).append("</pre>");
        }
        if (info.rewrittenSql != null && !info.rewrittenSql.isEmpty() && !info.rewrittenSql.equals(info.sql))
        {
            body.append("<h3>Legend SQL (rewritten)</h3><pre class=\"sql\">").append(Html.escape(info.rewrittenSql)).append("</pre>");
        }

        boolean isFail = "FAIL".equals(state);
        boolean isError = "ERROR".equals(state) || "BUG".equals(state);

        // 2. FAIL: differences, the SQL Legend actually generated/executed, the Pure lambda,
        // and the full expected-vs-actual result sets.
        if (isFail)
        {
            if (info.diffs != null && !info.diffs.isEmpty())
            {
                body.append("<h3 id=\"diffs-").append(path.toLowerCase()).append("\">Differences</h3><ul class=\"diffs\">");
                for (String diff : info.diffs)
                {
                    body.append("<li>").append(Html.escape(diff)).append("</li>");
                }
                body.append("</ul>");
            }
            if (info.generatedSql != null && !info.generatedSql.isEmpty())
            {
                body.append("<h3>Generated SQL (executed against Postgres)</h3><pre class=\"sql\">").append(Html.escape(info.generatedSql)).append("</pre>");
            }
            if (info.generatedLambda != null && !info.generatedLambda.isEmpty())
            {
                body.append("<h3>Generated Lambda (Pure expression)</h3><pre class=\"sql\">").append(Html.escape(info.generatedLambda)).append("</pre>");
            }
            if (info.expected != null)
            {
                body.append("<h3 id=\"expected-").append(path.toLowerCase()).append("\">Expected (Postgres)</h3>");
                body.append(matrixTable(info.expected));
            }
            if (info.actual != null)
            {
                body.append("<h3 id=\"actual-").append(path.toLowerCase()).append("\">Actual (Legend)</h3>");
                body.append(matrixTable(info.actual));
            }
        }

        // 3. ERROR/BUG: the SQL is already shown above; just the error message itself.
        if (isError && info.error != null && !info.error.isEmpty())
        {
            body.append("<h3 id=\"error-").append(path.toLowerCase()).append("\">Error</h3><pre class=\"err\">").append(Html.escape(info.error)).append("</pre>");
        }

        // Anything else not already covered above (e.g. a generatedSql/generatedLambda captured
        // outside the FAIL branch, or an error surfaced on a state we don't special-case).
        if (!isFail && info.generatedSql != null && !info.generatedSql.isEmpty())
        {
            body.append("<h3>Generated SQL</h3><pre class=\"sql\">").append(Html.escape(info.generatedSql)).append("</pre>");
        }
        if (!isFail && info.generatedLambda != null && !info.generatedLambda.isEmpty())
        {
            body.append("<h3>Generated Lambda</h3><pre class=\"sql\">").append(Html.escape(info.generatedLambda)).append("</pre>");
        }
        if (!isError && info.error != null && !info.error.isEmpty())
        {
            body.append("<h3 id=\"error-").append(path.toLowerCase()).append("\">Error</h3><pre class=\"err\">").append(Html.escape(info.error)).append("</pre>");
        }
    }

    private String matrixTable(ResultMatrix matrix)
    {
        StringBuilder sb = new StringBuilder("<div class=\"matrix-wrap\"><table class=\"matrix\"><thead><tr>");
        for (String col : matrix.getColumnNames())
        {
            sb.append("<th>").append(Html.escape(col)).append("</th>");
        }
        sb.append("</tr></thead><tbody>");
        for (List<Object> row : matrix.getRows())
        {
            sb.append("<tr>");
            for (Object v : row)
            {
                sb.append("<td>").append(Html.escape(v == null ? "NULL" : String.valueOf(v))).append("</td>");
            }
            sb.append("</tr>");
        }
        sb.append("</tbody></table></div>");
        return sb.toString();
    }

    private void appendKv(StringBuilder body, String key, String value)
    {
        body.append("<tr><th>").append(Html.escape(key)).append("</th><td>").append(Html.escape(value)).append("</td></tr>");
    }

    // ===================== Index =====================

    /**
     * Writes the scorecard + full category table to {@code overview.html}, which serves as
     * the default content of the master-detail {@code index.html}'s detail pane (and remains
     * directly linkable/bookmarkable on its own).
     */
    private void renderOverview(List<AreaSummary> areas) throws IOException
    {
        StringBuilder body = new StringBuilder();
        body.append("<h1>Legend SQL &#8680; PostgreSQL Parity</h1>");
        body.append("<p class=\"meta\">Generated ").append(Instant.now()).append(" &middot; Legend ").append(Html.escape(legendRevision)).append("</p>");

        int totalTds = 0;
        int totalRel = 0;
        int totalEntries = 0;
        int totalTdsUnsupported = 0;
        int totalRelUnsupported = 0;
        for (AreaSummary area : areas)
        {
            for (CategorySummary c : area.categories)
            {
                totalTds += c.tdsPass;
                totalRel += c.relPass;
                totalEntries += c.total;
                totalTdsUnsupported += c.tdsUnsupported;
                totalRelUnsupported += c.relUnsupported;
            }
        }
        int supportedTdsEntries = totalEntries - totalTdsUnsupported;
        int supportedRelEntries = totalEntries - totalRelUnsupported;
        body.append("<div class=\"scorecard\">");
        body.append(bar("TDS", totalEntries == 0 ? 0 : (100 * totalTds / totalEntries), totalTds, totalEntries));
        body.append(bar("Relation", totalEntries == 0 ? 0 : (100 * totalRel / totalEntries), totalRel, totalEntries));
        body.append(bar("TDS (supported only)", supportedTdsEntries == 0 ? 0 : (100 * totalTds / supportedTdsEntries), totalTds, supportedTdsEntries));
        body.append(bar("Relation (supported only)", supportedRelEntries == 0 ? 0 : (100 * totalRel / supportedRelEntries), totalRel, supportedRelEntries));
        body.append("</div>");

        body.append("<table class=\"index-table\"><tr><th>Area</th><th>Category</th><th>TDS</th><th>Relation</th><th>Tested</th></tr>");
        for (AreaSummary area : areas)
        {
            for (CategorySummary c : area.categories)
            {
                String docUrl = DocLinks.urlFor(c.category);
                String docLink = docUrl == null ? "" : " <a class=\"pg-doc-link\" href=\"" + Html.attr(docUrl) + "\">&#8599;</a>";
                int tdsPct = c.total == 0 ? 0 : (100 * c.tdsPass / c.total);
                int relPct = c.total == 0 ? 0 : (100 * c.relPass / c.total);
                body.append("<tr><td>").append(area.name).append("</td><td><a href=\"").append(c.href).append("\" target=\"_top\">")
                        .append(Html.escape(c.category)).append("</a>").append(docLink).append("</td><td>").append(tdsPct).append("%</td><td>")
                        .append(relPct).append("%</td><td>").append(c.tested).append(" / ").append(c.total).append("</td></tr>");
            }
        }
        body.append("</table>");

        writePage(outputDir + "/overview.html", pageShell("Legend SQL Parity", body.toString(), 0));
    }

    /**
     * Writes {@code index.html} as a master-detail shell: a left-hand sidebar listing every
     * area (Functions/Operators/Structural/Compositions/Format Tokens) with its category
     * pages underneath, and a detail iframe on the right that loads whichever page was
     * clicked (defaulting to {@code overview.html}).
     */
    private void renderMasterDetailIndex(List<AreaSummary> areas) throws IOException
    {
        Map<String, List<NavEntry>> byArea = new LinkedHashMap<>();
        for (NavEntry entry : nav)
        {
            byArea.computeIfAbsent(entry.area, k -> new ArrayList<>()).add(entry);
        }

        StringBuilder sidebar = new StringBuilder();
        sidebar.append("<nav class=\"sidebar\" aria-label=\"Parity site navigation\">");
        sidebar.append("<a class=\"nav-overview active\" href=\"overview.html\" target=\"detail-frame\" onclick=\"paritySelectNav(this)\">Overview</a>");
        for (Map.Entry<String, List<NavEntry>> areaEntry : byArea.entrySet())
        {
            sidebar.append("<div class=\"nav-area\"><div class=\"nav-area-title\">").append(Html.escape(areaEntry.getKey())).append("</div><ul class=\"nav-list\">");
            for (NavEntry item : areaEntry.getValue())
            {
                sidebar.append("<li><a href=\"").append(item.href).append("\" target=\"detail-frame\" onclick=\"paritySelectNav(this)\">")
                        .append(Html.escape(item.label)).append("</a></li>");
            }
            sidebar.append("</ul></div>");
        }
        sidebar.append("</nav>");

        String body = "<div class=\"master-detail\">" + sidebar
                + "<iframe id=\"detail-frame\" name=\"detail-frame\" class=\"detail-frame\" src=\"overview.html\" title=\"Detail\"></iframe>"
                + "</div>";

        writePage(outputDir + "/index.html", pageShell("Legend SQL Parity", body, 0, "md-main"));
    }

    private String bar(String label, int pct, int pass, int total)
    {
        return "<div class=\"bar-row\"><span class=\"bar-label\">" + label + "</span>"
                + "<div class=\"bar\"><div class=\"bar-fill\" style=\"width:" + pct + "%\"></div></div>"
                + "<span class=\"bar-pct\">" + pct + "% (" + pass + "/" + total + ")</span></div>";
    }

    // ===================== Shared row/page rendering =====================

    private String entryRow(String name, String signature, String tdsStatus, String relStatus, int pass, int total, boolean untested, String href)
    {
        StatusGlyph.Glyph tdsGlyph = StatusGlyph.forStatus(tdsStatus);
        StatusGlyph.Glyph relGlyph = StatusGlyph.forStatus(relStatus);
        String nameCell = href != null ? "<a href=\"../" + href + "\">" + Html.escape(name) + "</a>" : Html.escape(name);
        return "<tr class=\"entry-row\" data-untested=\"" + untested + "\" data-search=\"" + Html.attr((name + " " + signature).toLowerCase()) + "\">"
                + "<td>" + nameCell + "</td>"
                + "<td class=\"sig\">" + Html.escape(signature) + "</td>"
                + "<td class=\"" + tdsGlyph.cssClass + "\" aria-label=\"TDS: " + tdsGlyph.ariaLabel + "\">" + tdsGlyph.symbol + " " + StatusGlyph.label(tdsStatus, pass, total) + "</td>"
                + "<td class=\"" + relGlyph.cssClass + "\" aria-label=\"Relation: " + relGlyph.ariaLabel + "\">" + relGlyph.symbol + " " + StatusGlyph.label(relStatus, pass, total) + "</td>"
                + "<td>" + pass + " / " + total + "</td>"
                + "</tr>";
    }

    private String tableShell(String firstColLabel, String rows)
    {
        return "<table class=\"entries\"><thead><tr><th>" + firstColLabel + "</th><th>Signature</th><th>TDS</th><th>Relation</th><th>Coverage</th></tr></thead><tbody>"
                + rows + "</tbody></table>";
    }

    private String categoryPageBody(String category, String firstColLabel, String rows)
    {
        StringBuilder body = new StringBuilder();
        body.append("<h1>").append(Html.escape(category)).append("</h1>");
        String docUrl = DocLinks.urlFor(category);
        if (docUrl != null)
        {
            body.append("<p class=\"pg-doc-link\"><a href=\"").append(Html.attr(docUrl)).append("\">PostgreSQL docs &#8599;</a></p>");
        }
        body.append("<input type=\"text\" class=\"filter-box\" placeholder=\"Filter&hellip;\" oninput=\"parityFilter(this)\" aria-label=\"Filter table rows\"/>");
        body.append("<label class=\"show-untested\"><input type=\"checkbox\" class=\"show-untested-toggle\" onchange=\"parityToggleUntested(this)\"/> Show untested</label>");
        body.append(tableShell(firstColLabel, rows));
        return body.toString();
    }

    private List<TestLinkEntry> testEntries(FunctionCoverageMapper.SignatureCoverage cov)
    {
        List<TestLinkEntry> entries = new ArrayList<>();
        if (cov != null)
        {
            for (FunctionCoverageMapper.TestResultEntry e : cov.testDetails)
            {
                entries.add(new TestLinkEntry(e.testId, e.tdsState, e.relationState));
            }
        }
        return entries;
    }

    private List<TestLinkEntry> testEntries(OperatorCoverageMapper.SignatureCoverage cov)
    {
        List<TestLinkEntry> entries = new ArrayList<>();
        if (cov != null)
        {
            for (FunctionCoverageMapper.TestResultEntry e : cov.testDetails)
            {
                entries.add(new TestLinkEntry(e.testId, e.tdsState, e.relationState));
            }
        }
        return entries;
    }

    /**
     * Returns the {@code href} (relative to a category page, i.e. without the leading {@code ../})
     * that an entry-table row should link to: the single linked test's own permalink when there is
     * exactly one, or a synthetic group page listing every linked test's own status when there is
     * more than one ? so a PARTIAL/aggregate row never lands the user on a single test's page that
     * only tells part of the story.
     */
    private String groupHref(String groupSlug, String title, List<TestLinkEntry> entries)
    {
        if (entries.isEmpty())
        {
            return null;
        }
        if (entries.size() == 1)
        {
            return "test/" + entries.get(0).testId + ".html";
        }
        try
        {
            renderTestGroupPage(groupSlug, title, entries);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        return "test/group/" + groupSlug + ".html";
    }

    private void renderTestGroupPage(String groupSlug, String title, List<TestLinkEntry> entries) throws IOException
    {
        StringBuilder body = new StringBuilder();
        body.append("<h1>").append(Html.escape(title)).append("</h1>");
        body.append("<p class=\"meta\">").append(entries.size()).append(" linked tests</p>");
        body.append("<table class=\"entries\"><thead><tr><th>Test</th><th>TDS</th><th>Relation</th></tr></thead><tbody>");
        for (TestLinkEntry e : entries)
        {
            StatusGlyph.Glyph tdsGlyph = StatusGlyph.forStatus(e.tdsState);
            StatusGlyph.Glyph relGlyph = StatusGlyph.forStatus(e.relState);
            body.append("<tr><td><a href=\"../").append(e.testId).append(".html\">").append(Html.escape(e.testId)).append("</a></td>")
                    .append("<td class=\"").append(tdsGlyph.cssClass).append("\">").append(tdsGlyph.symbol).append(" ")
                    .append(e.tdsState == null ? "UNTESTED" : e.tdsState).append("</td>")
                    .append("<td class=\"").append(relGlyph.cssClass).append("\">").append(relGlyph.symbol).append(" ")
                    .append(e.relState == null ? "UNTESTED" : e.relState).append("</td></tr>");
        }
        body.append("</tbody></table>");
        writePage(outputDir + "/test/group/" + groupSlug + ".html", pageShell(title, body.toString(), 3));
    }

    private static class TestLinkEntry
    {
        final String testId;
        final String tdsState;
        final String relState;

        TestLinkEntry(String testId, String tdsState, String relState)
        {
            this.testId = testId;
            this.tdsState = tdsState;
            this.relState = relState;
        }
    }

    // ===================== Page shell / assets =====================

    /**
     * @param depth number of directory levels below the site root (0 = index.html, 1 = functions/foo.html, 2 = test/foo.html)
     */
    private String pageShell(String title, String body, int depth)
    {
        return pageShell(title, body, depth, "");
    }

    /**
     * @param depth     number of directory levels below the site root (0 = index.html, 1 = functions/foo.html,
     *                  2 = test/foo.html, 3 = test/group/foo.html)
     * @param mainClass extra CSS class(es) for the {@code <main>} element, or {@code ""} for none
     *                  (used by the master-detail {@code index.html} to opt out of the default
     *                  max-width/padding so its sidebar + iframe can fill the viewport)
     */
    private String pageShell(String title, String body, int depth, String mainClass)
    {
        StringBuilder relBuilder = new StringBuilder();
        for (int i = 1; i < depth; i++)
        {
            relBuilder.append("../");
        }
        String rel = depth == 0 ? "" : (depth == 1 ? "../" : relBuilder.toString());
        String mainAttrs = mainClass.isEmpty() ? "" : " class=\"" + mainClass + "\"";
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\"/>");
        sb.append("<title>").append(Html.escape(title)).append(" &middot; Legend SQL</title>");
        sb.append("<link rel=\"stylesheet\" href=\"").append(rel).append("assets/app.css\"/>");
        sb.append("</head><body>");
        sb.append("<header class=\"site-header\"><a href=\"").append(rel).append("index.html\">Legend SQL &#8680; PostgreSQL Parity</a></header>");
        sb.append("<main").append(mainAttrs).append(">").append(body).append("</main>");
        sb.append("<script src=\"").append(rel).append("assets/app.js\"></script>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private void writePage(String path, String content) throws IOException
    {
        try (java.io.Writer w = new java.io.OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8))
        {
            w.write(content);
        }
    }

    private void writeAssets() throws IOException
    {
        File assetsDir = new File(outputDir, "assets");
        assetsDir.mkdirs();
        copyResource("/site/assets/app.css", new File(assetsDir, "app.css"));
        copyResource("/site/assets/app.js", new File(assetsDir, "app.js"));
    }

    private void copyResource(String resourcePath, File target) throws IOException
    {
        try (InputStream is = DocumentationSiteGenerator.class.getResourceAsStream(resourcePath))
        {
            if (is == null)
            {
                LOGGER.warn("Missing bundled asset resource: {}", resourcePath);
                return;
            }
            try (OutputStream os = new FileOutputStream(target))
            {
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) != -1)
                {
                    os.write(buf, 0, n);
                }
            }
        }
    }

    // ===================== Small data holders =====================

    private static class AreaSummary
    {
        final String name;
        final String slug;
        final List<CategorySummary> categories = new ArrayList<>();

        AreaSummary(String name, String slug)
        {
            this.name = name;
            this.slug = slug;
        }
    }

    private static class CategorySummary
    {
        final String category;
        final String href;
        int total;
        int tested;
        int tdsPass;
        int relPass;
        int tdsUnsupported;
        int relUnsupported;

        CategorySummary(String category, String href)
        {
            this.category = category;
            this.href = href;
        }
    }

    private static boolean isUnsupported(String status)
    {
        return "NOT_APPLICABLE".equals(status) || "UNSUPPORTED".equals(status);
    }

    private static class NavEntry
    {
        final String area;
        final String label;
        final String href;

        NavEntry(String area, String label, String href)
        {
            this.area = area;
            this.label = label;
            this.href = href;
        }
    }
}




