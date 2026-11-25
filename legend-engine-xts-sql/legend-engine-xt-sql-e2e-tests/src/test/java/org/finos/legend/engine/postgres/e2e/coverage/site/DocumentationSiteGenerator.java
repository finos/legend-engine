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
import org.finos.legend.engine.postgres.e2e.ParityPath;
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
            boolean untested = "UNTESTED".equals(fn.tdsStatus) && "UNTESTED".equals(fn.relStatus) && "UNTESTED".equals(fn.grammarStatus);
            summary.total++;
            if (!untested)
            {
                summary.tested++;
            }
            summary.tally(ParityPath.TDS, fn.tdsStatus);
            summary.tally(ParityPath.RELATION, fn.relStatus);
            summary.tally(ParityPath.GRAMMAR, fn.grammarStatus);
            int total = fn.coverage != null ? fn.coverage.total : 0;
            int pass = fn.coverage != null ? fn.coverage.tdsPass : 0;
            int relPass = fn.coverage != null ? fn.coverage.relPass : 0;
            int grammarPass = fn.coverage != null ? fn.coverage.grammarPass : 0;
            String href = groupHref(slug + "__" + DocLinks.slugFor(fn.signature), fn.signature, testEntries(fn.coverage));
            rows.append(entryRow(fn.name, fn.signature, new String[] {fn.tdsStatus, fn.relStatus, fn.grammarStatus}, new int[] {pass, relPass, grammarPass}, total, untested, href));
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
            boolean untested = "UNTESTED".equals(op.tdsStatus) && "UNTESTED".equals(op.relStatus) && "UNTESTED".equals(op.grammarStatus);
            summary.total++;
            if (!untested)
            {
                summary.tested++;
            }
            summary.tally(ParityPath.TDS, op.tdsStatus);
            summary.tally(ParityPath.RELATION, op.relStatus);
            summary.tally(ParityPath.GRAMMAR, op.grammarStatus);
            int total = op.coverage != null ? op.coverage.total : 0;
            int pass = op.coverage != null ? op.coverage.tdsPass : 0;
            int relPass = op.coverage != null ? op.coverage.relPass : 0;
            int grammarPass = op.coverage != null ? op.coverage.grammarPass : 0;
            String sig = op.leftType + " " + op.name + " " + op.rightType + " \u2192 " + op.resultType;
            String href = groupHref(slug + "__" + DocLinks.slugFor(sig), sig, testEntries(op.coverage));
            rows.append(entryRow(op.name, sig, new String[] {op.tdsStatus, op.relStatus, op.grammarStatus}, new int[] {pass, relPass, grammarPass}, total, untested, href));
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
            String grammarStatus = fc.grammarStatus();
            boolean untested = fc.total == 0;
            summary.total++;
            if (!untested)
            {
                summary.tested++;
            }
            summary.tally(ParityPath.TDS, tdsStatus);
            summary.tally(ParityPath.RELATION, relStatus);
            summary.tally(ParityPath.GRAMMAR, grammarStatus);
            List<TestLinkEntry> entries = new ArrayList<>();
            for (StructuralParityReport.TestEntry te : fc.tests)
            {
                entries.add(new TestLinkEntry(te.testId, te.tdsState, te.relationState, te.grammarState));
            }
            String href = groupHref(slug + "__" + DocLinks.slugFor(fc.featureName), fc.featureName, entries);
            rows.append(entryRow(fc.featureName, fc.categoryName, new String[] {tdsStatus, relStatus, grammarStatus}, new int[] {fc.tdsPass, fc.relPass, fc.grammarPass}, fc.total, untested, href));
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
                summary.tally(ParityPath.TDS, te.tdsState);
                summary.tally(ParityPath.RELATION, te.relState);
                summary.tally(ParityPath.GRAMMAR, te.grammarState);
                List<TestLinkEntry> entries = new ArrayList<>();
                for (String id : te.testIds)
                {
                    entries.add(new TestLinkEntry(id, te.tdsState, te.relState, te.grammarState));
                }
                String href = groupHref("token__" + DocLinks.slugFor(fEntry.getKey()) + "__" + DocLinks.slugFor(te.token), te.token, entries);
                rows.append(entryRow(te.token, fEntry.getKey(), new String[] {te.tdsState, te.relState, te.grammarState}, new int[] {0, 0, 0}, te.testIds.size(), untested, href));
            }
            body.append(tableShell("Token", rows.toString()));
        }
        writePage(outputDir + "/format-tokens.html", pageShell("Format Tokens", body.toString(), 1));
        return summary;
    }

    // ===================== Test detail pages =====================

    private void renderTestPage(TestCaseLoader.TestCase tc) throws IOException
    {
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
        for (ParityPath path : ParityPath.values())
        {
            body.append(statusRow(path.label(), testIndex.get(tc.id, path.label()), path.expectedStatus(tc)));
        }
        body.append("</table>");

        body.append("<h2>Reference SQL</h2><pre class=\"sql\">").append(Html.escape(tc.sql)).append("</pre>");
        for (ParityPath path : ParityPath.values())
        {
            appendPathDetail(body, path.label(), testIndex.get(tc.id, path.label()));
        }

        writePage(outputDir + "/test/" + testFileName(tc.id) + ".html", pageShell(tc.id, body.toString(), 2));
    }

    /**
     * Maps a test id to the base name of its detail page (without extension), stripping characters
     * that artifact upload rejects (e.g. {@code "}) and disambiguating ids that differ only by case
     * (e.g. {@code Day} vs {@code day}) so the pages never collide on a case-insensitive filesystem.
     */
    private static String testFileName(String id)
    {
        String sanitized = id.replaceAll("[^A-Za-z0-9_-]", "_");
        if (sanitized.equals(sanitized.toLowerCase()) && sanitized.equals(id))
        {
            return id;
        }
        return sanitized.toLowerCase() + "__" + Integer.toHexString(id.hashCode() & 0x7fffffff);
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

        int[] totalPass = new int[ParityPath.values().length];
        int[] totalUnsupported = new int[ParityPath.values().length];
        int totalEntries = 0;
        for (AreaSummary area : areas)
        {
            for (CategorySummary c : area.categories)
            {
                totalEntries += c.total;
                for (ParityPath path : ParityPath.values())
                {
                    totalPass[path.ordinal()] += c.pass(path);
                    totalUnsupported[path.ordinal()] += c.unsupported(path);
                }
            }
        }
        body.append("<div class=\"scorecard\">");
        for (ParityPath path : ParityPath.values())
        {
            int pass = totalPass[path.ordinal()];
            body.append(bar(path.label(), totalEntries == 0 ? 0 : (100 * pass / totalEntries), pass, totalEntries));
        }
        for (ParityPath path : ParityPath.values())
        {
            int pass = totalPass[path.ordinal()];
            int supported = totalEntries - totalUnsupported[path.ordinal()];
            body.append(bar(path.label() + " (supported only)", supported == 0 ? 0 : (100 * pass / supported), pass, supported));
        }
        body.append("</div>");

        body.append("<table class=\"index-table\"><tr><th>Area</th><th>Category</th><th>TDS</th><th>Relation</th><th>Grammar</th><th>Tested</th></tr>");
        for (AreaSummary area : areas)
        {
            for (CategorySummary c : area.categories)
            {
                String docUrl = DocLinks.urlFor(c.category);
                String docLink = docUrl == null ? "" : " <a class=\"pg-doc-link\" href=\"" + Html.attr(docUrl) + "\">&#8599;</a>";
                body.append("<tr><td>").append(area.name).append("</td><td><a href=\"").append(c.href).append("\" target=\"_top\">")
                        .append(Html.escape(c.category)).append("</a>").append(docLink).append("</td>");
                for (ParityPath path : ParityPath.values())
                {
                    int pct = c.total == 0 ? 0 : (100 * c.pass(path) / c.total);
                    body.append("<td>").append(pct).append("%</td>");
                }
                body.append("<td>").append(c.tested).append(" / ").append(c.total).append("</td></tr>");
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

    private String entryRow(String name, String signature, String[] statuses, int[] passes, int total, boolean untested, String href)
    {
        String nameCell = href != null ? "<a href=\"../" + href + "\">" + Html.escape(name) + "</a>" : Html.escape(name);
        StringBuilder row = new StringBuilder();
        row.append("<tr class=\"entry-row\" data-untested=\"").append(untested).append("\" data-search=\"")
                .append(Html.attr((name + " " + signature).toLowerCase())).append("\">")
                .append("<td>").append(nameCell).append("</td>")
                .append("<td class=\"sig\">").append(Html.escape(signature)).append("</td>");
        for (ParityPath path : ParityPath.values())
        {
            String status = statuses[path.ordinal()];
            StatusGlyph.Glyph glyph = StatusGlyph.forStatus(status);
            row.append("<td class=\"").append(glyph.cssClass).append("\" aria-label=\"").append(path.label()).append(": ")
                    .append(glyph.ariaLabel).append("\">").append(glyph.symbol).append(" ")
                    .append(StatusGlyph.label(status, passes[path.ordinal()], total)).append("</td>");
        }
        row.append("<td>").append(passes[0]).append(" / ").append(total).append("</td>").append("</tr>");
        return row.toString();
    }

    private String tableShell(String firstColLabel, String rows)
    {
        return "<table class=\"entries\"><thead><tr><th>" + firstColLabel + "</th><th>Signature</th><th>TDS</th><th>Relation</th><th>Grammar</th><th>Coverage</th></tr></thead><tbody>"
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
                entries.add(new TestLinkEntry(e.testId, e.tdsState, e.relationState, e.grammarState));
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
                entries.add(new TestLinkEntry(e.testId, e.tdsState, e.relationState, e.grammarState));
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
            return "test/" + testFileName(entries.get(0).testId) + ".html";
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
        body.append("<table class=\"entries\"><thead><tr><th>Test</th><th>TDS</th><th>Relation</th><th>Grammar</th></tr></thead><tbody>");
        for (TestLinkEntry e : entries)
        {
            body.append("<tr><td><a href=\"../").append(testFileName(e.testId)).append(".html\">").append(Html.escape(e.testId)).append("</a></td>");
            for (ParityPath path : ParityPath.values())
            {
                String state = e.states[path.ordinal()];
                StatusGlyph.Glyph glyph = StatusGlyph.forStatus(state);
                body.append("<td class=\"").append(glyph.cssClass).append("\">").append(glyph.symbol).append(" ")
                        .append(state == null ? "UNTESTED" : state).append("</td>");
            }
            body.append("</tr>");
        }
        body.append("</tbody></table>");
        writePage(outputDir + "/test/group/" + groupSlug + ".html", pageShell(title, body.toString(), 3));
    }

    private static class TestLinkEntry
    {
        final String testId;
        final String[] states;

        TestLinkEntry(String testId, String tdsState, String relState, String grammarState)
        {
            this.testId = testId;
            this.states = new String[] {tdsState, relState, grammarState};
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
        private final int[] passByPath = new int[ParityPath.values().length];
        private final int[] unsupportedByPath = new int[ParityPath.values().length];

        CategorySummary(String category, String href)
        {
            this.category = category;
            this.href = href;
        }

        void tally(ParityPath path, String status)
        {
            if ("PASS".equals(status))
            {
                passByPath[path.ordinal()]++;
            }
            if (isUnsupported(status))
            {
                unsupportedByPath[path.ordinal()]++;
            }
        }

        int pass(ParityPath path)
        {
            return passByPath[path.ordinal()];
        }

        int unsupported(ParityPath path)
        {
            return unsupportedByPath[path.ordinal()];
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




