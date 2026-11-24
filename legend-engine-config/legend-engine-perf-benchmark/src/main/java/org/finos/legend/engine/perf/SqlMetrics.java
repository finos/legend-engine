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

package org.finos.legend.engine.perf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The size of the SQL in a plan: characters, selects, joins, left joins and aliased columns, summed over every statement.
 * Unlike a timing it is the same on every machine, so a suite can hold it to a recorded value and notice that the SQL
 * optimizer has stopped shrinking a query it used to shrink.
 *
 * Quoted identifiers and string literals are blanked before counting, so a column named "select" or a value containing
 * "join" does not count.
 */
final class SqlMetrics
{
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern QUOTED = Pattern.compile("\"[^\"]*\"|'(?:[^']|'')*'");
    private static final Pattern SELECT = Pattern.compile("\\bselect\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern JOIN = Pattern.compile("\\bjoin\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern LEFT_JOIN = Pattern.compile("\\bleft\\s+(?:outer\\s+)?join\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ALIAS = Pattern.compile("\\bas\\b", Pattern.CASE_INSENSITIVE);

    private SqlMetrics()
    {
    }

    static Map<String, Object> of(String planJson)
    {
        List<String> statements = new ArrayList<>();
        try
        {
            collect(MAPPER.readTree(planJson), statements);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        long chars = 0;
        long selects = 0;
        long joins = 0;
        long leftJoins = 0;
        long columns = 0;
        for (String statement : statements)
        {
            String text = QUOTED.matcher(statement).replaceAll("\"\"");
            chars += statement.length();
            selects += count(SELECT, text);
            joins += count(JOIN, text);
            leftJoins += count(LEFT_JOIN, text);
            columns += count(ALIAS, text);
        }
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("statements", statements.size());
        metrics.put("chars", chars);
        metrics.put("selects", selects);
        metrics.put("joins", joins);
        metrics.put("leftJoins", leftJoins);
        metrics.put("columns", columns);
        return metrics;
    }

    private static void collect(JsonNode node, List<String> statements)
    {
        if (node.isObject())
        {
            node.fields().forEachRemaining(field ->
            {
                if (field.getKey().equals("sqlQuery") && field.getValue().isTextual())
                {
                    statements.add(field.getValue().asText());
                }
                else
                {
                    collect(field.getValue(), statements);
                }
            });
        }
        else if (node.isArray())
        {
            node.forEach(child -> collect(child, statements));
        }
    }

    private static long count(Pattern pattern, String text)
    {
        Matcher matcher = pattern.matcher(text);
        long found = 0;
        while (matcher.find())
        {
            found++;
        }
        return found;
    }
}
