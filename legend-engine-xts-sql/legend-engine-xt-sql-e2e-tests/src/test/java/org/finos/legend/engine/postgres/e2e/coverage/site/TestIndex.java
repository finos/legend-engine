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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.postgres.e2e.ParityReport;
import org.finos.legend.engine.postgres.e2e.ResultMatrix;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-test, per-path lookup used by {@link DocumentationSiteGenerator} to render test detail pages
 * and by the category pages' "first test" links.
 *
 * <p>Prefers the in-memory {@link ParityReport.TestResult} list (has the full, uncapped
 * {@link ResultMatrix} ? plan &sect;8.2 "full fidelity, no truncation") and falls back to
 * {@code parity-report.json} (sql/rewrittenSql/generatedSql/generatedLambda/error/diffs only,
 * no matrices since those fields are transient) when only the JSON artefact is available, e.g.
 * in the golden test. {@code diffs} and {@code error} are only ever present on the "failures"
 * array in the JSON (the flat "results" array omits them for every PASS row), so both arrays
 * are consulted.
 */
final class TestIndex
{
    private final Map<String, TestPathInfo> byIdAndPath = new HashMap<>();

    static final class TestPathInfo
    {
        String state;
        String sql;
        String rewrittenSql;
        String generatedSql;
        String generatedLambda;
        String error;
        List<String> diffs;
        ResultMatrix expected;
        ResultMatrix actual;
    }

    TestPathInfo get(String testId, String path)
    {
        return byIdAndPath.get(testId + "|" + path);
    }

    private TestPathInfo entryFor(String testId, String path)
    {
        return byIdAndPath.computeIfAbsent(testId + "|" + path, k -> new TestPathInfo());
    }

    static TestIndex build(File parityReportJson, List<ParityReport.TestResult> inMemoryResults, ObjectMapper mapper) throws IOException
    {
        TestIndex index = new TestIndex();

        if (parityReportJson != null && parityReportJson.exists())
        {
            JsonNode report = mapper.readTree(parityReportJson);
            JsonNode results = report.get("results");
            if (results != null)
            {
                for (JsonNode r : results)
                {
                    String id = r.has("id") ? r.get("id").asText() : "";
                    String path = r.has("path") ? r.get("path").asText() : "";
                    TestPathInfo info = index.entryFor(id, path);
                    info.state = r.has("state") ? r.get("state").asText() : null;
                    info.sql = r.has("sql") ? r.get("sql").asText() : null;
                    info.rewrittenSql = r.has("rewrittenSql") ? r.get("rewrittenSql").asText() : null;
                    info.generatedSql = r.has("generatedSql") ? r.get("generatedSql").asText() : null;
                    info.generatedLambda = r.has("generatedLambda") ? r.get("generatedLambda").asText() : null;
                }
            }
            JsonNode failures = report.get("failures");
            if (failures != null)
            {
                for (JsonNode f : failures)
                {
                    String id = f.has("id") ? f.get("id").asText() : "";
                    String path = f.has("path") ? f.get("path").asText() : "";
                    TestPathInfo info = index.entryFor(id, path);
                    info.error = f.has("error") ? f.get("error").asText() : null;
                    if (info.sql == null && f.has("sql"))
                    {
                        info.sql = f.get("sql").asText();
                    }
                    if (info.rewrittenSql == null && f.has("rewrittenSql"))
                    {
                        info.rewrittenSql = f.get("rewrittenSql").asText();
                    }
                    if (info.generatedSql == null && f.has("generatedSql"))
                    {
                        info.generatedSql = f.get("generatedSql").asText();
                    }
                    if (info.generatedLambda == null && f.has("generatedLambda"))
                    {
                        info.generatedLambda = f.get("generatedLambda").asText();
                    }
                    JsonNode diffsNode = f.get("diffs");
                    if (diffsNode != null && diffsNode.isArray())
                    {
                        List<String> diffs = new ArrayList<>();
                        for (JsonNode d : diffsNode)
                        {
                            diffs.add(d.asText());
                        }
                        info.diffs = diffs;
                    }
                }
            }
        }

        // In-memory results (when the generator runs in the same JVM as the test suite)
        // carry the full expected/actual ResultMatrix, which is transient and therefore
        // never serialized into parity-report.json.
        if (inMemoryResults != null)
        {
            for (ParityReport.TestResult r : inMemoryResults)
            {
                TestPathInfo info = index.entryFor(r.id, r.path);
                info.state = r.state;
                info.sql = r.sql;
                info.rewrittenSql = r.rewrittenSql;
                info.generatedSql = r.generatedSql;
                info.generatedLambda = r.generatedLambda;
                info.error = r.error;
                info.diffs = r.diffs;
                info.expected = r.expectedResult;
                info.actual = r.actualResult;
            }
        }

        return index;
    }
}


