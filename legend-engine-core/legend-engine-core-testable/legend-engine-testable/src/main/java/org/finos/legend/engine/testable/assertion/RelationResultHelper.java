// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.testable.assertion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.finos.legend.engine.protocol.pure.v1.model.data.relation.RelationElement;
import org.finos.legend.engine.protocol.pure.v1.model.data.relation.RelationRowTestData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility class for converting between RelationElement, JSON, and TDS string formats.
 * Used by the test assertion evaluator for EqualToRelation assertions.
 */
public class RelationResultHelper
{
    /**
     * Convert a RelationElement to a JSON array-of-objects string.
     * Handles type coercion: values that parse as integers become JSON integers,
     * values that parse as doubles become JSON doubles, "null" or empty becomes JSON null,
     * and everything else stays a JSON string.
     */
    public static String relationElementToJson(RelationElement element) throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode arrayNode = mapper.createArrayNode();

        List<String> columns = element.columns != null ? element.columns : Collections.emptyList();
        List<RelationRowTestData> rows = element.rows != null ? element.rows : Collections.emptyList();

        for (RelationRowTestData row : rows)
        {
            ObjectNode objectNode = mapper.createObjectNode();
            for (int i = 0; i < columns.size(); i++)
            {
                String colName = columns.get(i).trim();
                String value = (i < row.values.size()) ? row.values.get(i).trim() : "";

                if (value.isEmpty() || "null".equalsIgnoreCase(value))
                {
                    objectNode.putNull(colName);
                }
                else
                {
                    // Try integer first
                    try
                    {
                        long longVal = Long.parseLong(value);
                        objectNode.put(colName, longVal);
                        continue;
                    }
                    catch (NumberFormatException ignored)
                    {
                    }

                    // Try double
                    try
                    {
                        double doubleVal = Double.parseDouble(value);
                        objectNode.put(colName, doubleVal);
                        continue;
                    }
                    catch (NumberFormatException ignored)
                    {
                    }

                    // Try boolean
                    if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value))
                    {
                        objectNode.put(colName, Boolean.parseBoolean(value));
                    }
                    else
                    {
                        objectNode.put(colName, value);
                    }
                }
            }
            arrayNode.add(objectNode);
        }

        return mapper.writeValueAsString(arrayNode);
    }

    /**
     * Format a RelationElement as a human-readable TDS table string.
     * Example:
     * <pre>
     * id | firstName | lastName
     * 1  | John      | Smith
     * 2  | Jane      | Doe
     * </pre>
     */
    public static String relationElementToTdsString(RelationElement element)
    {
        List<String> columns = element.columns != null ? element.columns : Collections.emptyList();
        List<RelationRowTestData> rows = element.rows != null ? element.rows : Collections.emptyList();

        List<List<String>> allRows = new ArrayList<>();
        allRows.add(columns);
        for (RelationRowTestData row : rows)
        {
            List<String> values = new ArrayList<>();
            for (int i = 0; i < columns.size(); i++)
            {
                values.add(i < row.values.size() ? row.values.get(i).trim() : "");
            }
            allRows.add(values);
        }

        return formatTable(allRows, columns.size());
    }

    /**
     * Parse a JSON array-of-objects and format it as a TDS table string using the given column names.
     * Used for formatting the actual result in error messages.
     */
    public static String jsonToTdsString(String json, List<String> columns) throws IOException
    {
        ObjectMapper mapper = TestAssertionHelper.buildObjectMapperForJSONComparison();
        JsonNode root = mapper.readTree(json);

        List<List<String>> allRows = new ArrayList<>();
        allRows.add(columns);

        if (root.isArray())
        {
            for (JsonNode element : root)
            {
                List<String> row = new ArrayList<>();
                for (String col : columns)
                {
                    JsonNode val = element.get(col.trim());
                    if (val == null || val.isNull())
                    {
                        row.add("null");
                    }
                    else
                    {
                        row.add(val.isTextual() ? val.textValue() : val.toString());
                    }
                }
                allRows.add(row);
            }
        }

        return formatTable(allRows, columns.size());
    }

    private static String formatTable(List<List<String>> rows, int numCols)
    {
        // Compute max widths
        int[] maxWidths = new int[numCols];
        for (List<String> row : rows)
        {
            for (int i = 0; i < numCols && i < row.size(); i++)
            {
                String val = row.get(i) != null ? row.get(i) : "";
                maxWidths[i] = Math.max(maxWidths[i], val.length());
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < rows.size(); r++)
        {
            List<String> row = rows.get(r);
            for (int c = 0; c < numCols; c++)
            {
                if (c > 0)
                {
                    sb.append(" | ");
                }
                String val = (c < row.size() && row.get(c) != null) ? row.get(c) : "";
                sb.append(padRight(val, maxWidths[c]));
            }
            if (r < rows.size() - 1)
            {
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private static String padRight(String s, int width)
    {
        if (s.length() >= width)
        {
            return s;
        }
        StringBuilder sb = new StringBuilder(width);
        sb.append(s);
        for (int i = s.length(); i < width; i++)
        {
            sb.append(' ');
        }
        return sb.toString();
    }
}

