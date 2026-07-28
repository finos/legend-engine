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
import org.finos.legend.engine.protocol.pure.m3.relation.Column;
import org.finos.legend.engine.protocol.pure.m3.type.Type;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.PackageableType;
import org.finos.legend.engine.protocol.pure.v1.model.data.relation.RelationElement;
import org.finos.legend.engine.protocol.pure.v1.model.data.relation.RelationRowTestData;
import org.finos.legend.pure.runtime.java.extension.functions.shared.string.CsvParseHelper;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RelationResultHelper
{

    private enum ColumnKind
    {
        TEXT,       // String, Varchar, Char, StrictDate, DateTime, Date, LatestDate, Byte, ...
        INTEGER,    // Integer, Int, TinyInt, SmallInt, BigInt, and unsigned variants
        FLOAT,      // Float, Float4, Double
        DECIMAL,    // Decimal, Numeric
        BOOLEAN,    // Boolean
        NUMBER,     // meta::pure::metamodel::type::Number
        UNKNOWN     // anything else: fall back to guessing
    }

    private enum PureTypeKind
    {
        // map pure types to ColumnKind
        STRING("String", ColumnKind.TEXT),
        INTEGER("Integer", ColumnKind.INTEGER),
        FLOAT("Float", ColumnKind.FLOAT),
        DECIMAL("Decimal", ColumnKind.DECIMAL),
        NUMBER("Number", ColumnKind.NUMBER),
        BOOLEAN("Boolean", ColumnKind.BOOLEAN),
        DATE("Date", ColumnKind.TEXT),
        STRICT_DATE("StrictDate", ColumnKind.TEXT),
        DATE_TIME("DateTime", ColumnKind.TEXT),
        LATEST_DATE("LatestDate", ColumnKind.TEXT),
        STRICT_TIME("StrictTime", ColumnKind.TEXT),
        BYTE("Byte", ColumnKind.TEXT),

        // Precise primitives
        P_VARCHAR("meta::pure::precisePrimitives::Varchar", ColumnKind.TEXT),
        P_CHAR("meta::pure::precisePrimitives::Char", ColumnKind.TEXT),
        P_TIMESTAMP("meta::pure::precisePrimitives::Timestamp", ColumnKind.TEXT),
        P_TINY_INT("meta::pure::precisePrimitives::TinyInt", ColumnKind.INTEGER),
        P_U_TINY_INT("meta::pure::precisePrimitives::UTinyInt", ColumnKind.INTEGER),
        P_SMALL_INT("meta::pure::precisePrimitives::SmallInt", ColumnKind.INTEGER),
        P_U_SMALL_INT("meta::pure::precisePrimitives::USmallInt", ColumnKind.INTEGER),
        P_INT("meta::pure::precisePrimitives::Int", ColumnKind.INTEGER),
        P_U_INT("meta::pure::precisePrimitives::UInt", ColumnKind.INTEGER),
        P_BIG_INT("meta::pure::precisePrimitives::BigInt", ColumnKind.INTEGER),
        P_U_BIG_INT("meta::pure::precisePrimitives::UBigInt", ColumnKind.INTEGER),
        P_FLOAT4("meta::pure::precisePrimitives::Float4", ColumnKind.FLOAT),
        P_DOUBLE("meta::pure::precisePrimitives::Double", ColumnKind.FLOAT),
        P_NUMERIC("meta::pure::precisePrimitives::Numeric", ColumnKind.DECIMAL);

        private static final Map<String, ColumnKind> BY_FULL_PATH;

        static
        {
            Map<String, ColumnKind> byFullPath = new HashMap<>();
            for (PureTypeKind t : values())
            {
                byFullPath.put(t.fullPath, t.kind);
            }
            BY_FULL_PATH = Collections.unmodifiableMap(byFullPath);
        }

        final String fullPath;
        final ColumnKind kind;

        PureTypeKind(String fullPath, ColumnKind kind)
        {
            this.fullPath = fullPath;
            this.kind = kind;
        }

        static ColumnKind kindOf(String fullPath)
        {
            if (fullPath == null)
            {
                return ColumnKind.UNKNOWN;
            }
            ColumnKind kind = BY_FULL_PATH.get(fullPath);
            return kind == null ? ColumnKind.UNKNOWN : kind;
        }
    }

    private static List<List<String>> parseRelationElementAsCsv(RelationElement element)
    {
        List<String> columns = element.columns != null ? element.columns : Collections.emptyList();
        List<RelationRowTestData> rows = element.rows != null ? element.rows : Collections.emptyList();

        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", columns));
        for (RelationRowTestData row : rows)
        {
            csv.append("\n").append(String.join(",", row.values));
        }
        return CsvParseHelper.parseCSV(csv.toString());
    }

    public static String relationElementToJson(RelationElement element) throws IOException
    {
        return relationElementToJson(element, null);
    }

    public static String relationElementToJson(RelationElement element, List<Column> columnTypes) throws IOException
    {
        List<List<String>> parsed = parseRelationElementAsCsv(element);
        if (parsed.isEmpty())
        {
            return "[]";
        }

        List<String> headerColumns = parsed.get(0);
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode arrayNode = mapper.createArrayNode();

        ColumnKind[] kinds = resolveColumnKinds(headerColumns, columnTypes);

        for (int r = 1; r < parsed.size(); r++)
        {
            List<String> rowValues = parsed.get(r);
            ObjectNode objectNode = mapper.createObjectNode();
            for (int i = 0; i < headerColumns.size(); i++)
            {
                String colName = headerColumns.get(i);
                String value = (i < rowValues.size()) ? rowValues.get(i) : "";
                putTypedValue(objectNode, colName, value, kinds[i]);
            }
            arrayNode.add(objectNode);
        }

        return mapper.writeValueAsString(arrayNode);
    }

    private static ColumnKind[] resolveColumnKinds(List<String> headerColumns, List<Column> columnTypes)
    {
        ColumnKind[] kinds = new ColumnKind[headerColumns.size()];
        boolean columnTypesPassed = columnTypes != null && !columnTypes.isEmpty();

        Map<String, ColumnKind> byName = Collections.emptyMap();
        if (columnTypesPassed)
        {
            byName = new HashMap<>();
            for (Column c : columnTypes)
            {
                if (c != null && c.name != null)
                {
                    byName.put(c.name.trim(), kindOf(c));
                }
            }
        }
        for (int i = 0; i < headerColumns.size(); i++)
        {
            String headerName = headerColumns.get(i) == null ? "" : headerColumns.get(i).trim();
            ColumnKind kind = byName.get(headerName);
            if (kind == null)
            {
                kind = columnTypesPassed ? ColumnKind.TEXT : ColumnKind.UNKNOWN;
            }
            kinds[i] = kind;
        }
        return kinds;
    }

    private static ColumnKind kindOf(Column column)
    {
        if (column == null || column.genericType == null || column.genericType.rawType == null)
        {
            return ColumnKind.UNKNOWN;
        }
        Type rawType = column.genericType.rawType;
        if (!(rawType instanceof PackageableType))
        {
            return ColumnKind.UNKNOWN;
        }
        return PureTypeKind.kindOf(((PackageableType) rawType).fullPath);
    }

    private static void putTypedValue(ObjectNode objectNode, String colName, String value, ColumnKind kind)
    {
        if (value == null || value.isEmpty() || "null".equalsIgnoreCase(value))
        {
            objectNode.putNull(colName);
            return;
        }
        switch (kind)
        {
            case TEXT:
                objectNode.put(colName, value);
                return;
            case INTEGER:
                try
                {
                    objectNode.put(colName, Long.parseLong(value.trim()));
                }
                catch (NumberFormatException e)
                {
                    // Preserve original text so the mismatch surfaces meaningfully in the failure message.
                    objectNode.put(colName, value);
                }
                return;
            case FLOAT:
                try
                {
                    objectNode.put(colName, Double.parseDouble(value.trim()));
                }
                catch (NumberFormatException e)
                {
                    objectNode.put(colName, value);
                }
                return;
            case DECIMAL:
                try
                {
                    objectNode.put(colName, new BigDecimal(value.trim()));
                }
                catch (NumberFormatException e)
                {
                    objectNode.put(colName, value);
                }
                return;
            case BOOLEAN:
                String trimmed = value.trim();
                if ("true".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed))
                {
                    objectNode.put(colName, Boolean.parseBoolean(trimmed));
                }
                else
                {
                    objectNode.put(colName, value);
                }
                return;
            case NUMBER:
                // Number can be integer or decimal at runtime — prefer integer when it fits.
                try
                {
                    objectNode.put(colName, Long.parseLong(value.trim()));
                    return;
                }
                catch (NumberFormatException ignored)
                {
                    // fall through
                }
                try
                {
                    objectNode.put(colName, new BigDecimal(value.trim()));
                }
                catch (NumberFormatException e)
                {
                    objectNode.put(colName, value);
                }
                return;
            case UNKNOWN:
            default:
                putGuessedValue(objectNode, colName, value);
        }
    }

    private static void putGuessedValue(ObjectNode objectNode, String colName, String value)
    {
        try
        {
            objectNode.put(colName, Long.parseLong(value));
            return;
        }
        catch (NumberFormatException ignored)
        {
        }
        try
        {
            objectNode.put(colName, Double.parseDouble(value));
            return;
        }
        catch (NumberFormatException ignored)
        {
        }
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value))
        {
            objectNode.put(colName, Boolean.parseBoolean(value));
        }
        else
        {
            objectNode.put(colName, value);
        }
    }

    public static String relationElementToTdsString(RelationElement element)
    {
        List<List<String>> parsed = parseRelationElementAsCsv(element);
        if (parsed.isEmpty())
        {
            return "";
        }
        return formatTable(parsed, parsed.get(0).size());
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

