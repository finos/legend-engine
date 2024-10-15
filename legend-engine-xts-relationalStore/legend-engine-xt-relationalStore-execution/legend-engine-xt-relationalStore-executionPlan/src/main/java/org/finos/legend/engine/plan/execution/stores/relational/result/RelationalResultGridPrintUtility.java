// Copyright 2024 Goldman Sachs
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
//

package org.finos.legend.engine.plan.execution.stores.relational.result;

import java.sql.ResultSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.result.SQLResultColumn;

public class RelationalResultGridPrintUtility
{
    public static String prettyGridPrint(ResultSet resultSet, List<String> columnNames, List<String> columnTypes, int maxRowSize, int maxColSize)
    {
        List<List<Object>> values = Lists.mutable.empty();

        try
        {

            // collect data
            for (int i = 0; i < columnNames.size(); i++)
            {
                values.add(Lists.mutable.empty());
            }
            while (resultSet.next())
            {
                for (int i = 1; i <= columnNames.size(); i++)
                {
                    String value = resultSet.getObject(i) == null ? "" : resultSet.getObject(i).toString();
                    values.get(i - 1).add(value);
                }
            }

            return prettyGridPrint(columnNames, columnTypes, maxRowSize, maxColSize, values);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static String prettyGridPrint(RelationalResult relationalResult, int maxRowSize, int maxColSize)
    {
        try (ResultSet rs = relationalResult.resultSet)
        {
            return prettyGridPrint(rs, relationalResult.sqlColumns, ListIterate.collect(relationalResult.getSQLResultColumns(), col -> col.dataType), maxRowSize, maxColSize);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static String prettyGridPrint(RealizedRelationalResult resultSet, int maxRowSize, int maxColSize)
    {
        List<List<Object>> data = resultSet.transformedRows;
        List<String> columnNames = resultSet.columns.stream().map(SQLResultColumn::getNonQuotedLabel).collect(Collectors.toList());
        List<String> columnTypes = resultSet.columns.stream().map(x -> x.dataType).collect(Collectors.toList());


        List<List<Object>> rows = Lists.mutable.empty();

        // collect data in columnar fashion
        for (int i = 0; i < columnNames.size(); i++)
        {
            rows.add(Lists.mutable.empty());
        }
        for (List<Object> row : data)
        {
            for (int i = 0; i < columnNames.size(); i++)
            {
                rows.get(i).add(row.get(i));
            }
        }

        return prettyGridPrint(columnNames, columnTypes, maxRowSize, maxColSize, rows);
    }

    private static String prettyGridPrint(List<String> columnNames, List<String> columnTypes, int maxRowSize, int maxColSize, List<List<Object>> values)
    {
        int columnCount = columnNames.size();

        MutableList<Integer> size = Lists.mutable.empty();

        // determine the max size for each column
        for (int i = 0; i < columnCount; i++)
        {
            int currMax = Math.max(columnNames.get(i).length(), columnTypes.get(i).length());
            size.add(Lists.adapt(values.get(i)).injectInto(currMax, (IntObjectToIntFunction<? super Object>) (a, b) -> Math.max(Objects.toString(b).length(), a)));
        }
        size = Lists.mutable.withAll(size.collect(s -> Math.min(maxColSize, s + 2)));

        // print the result
        StringBuilder builder = new StringBuilder();

        drawSeparation(builder, columnCount, size);
        drawRow(builder, columnCount, size, columnNames::get, maxColSize);
        drawRow(builder, columnCount, size, columnTypes::get, maxColSize);
        drawSeparation(builder, columnCount, size);

        int rows = values.get(0).size();
        if (rows <= maxRowSize)
        {
            for (int k = 0; k < rows; k++)
            {
                final int fk = k;
                drawRow(builder, columnCount, size, i -> Objects.toString(values.get(i).get(fk)), maxColSize);
            }
        }
        else
        {
            int topRows = (int) Math.ceil((float) maxRowSize / 2);
            int bottomRows = maxRowSize - topRows;
            for (int k = 0; k < topRows; k++)
            {
                final int fk = k;
                drawRow(builder, columnCount, size, i -> Objects.toString(values.get(i).get(fk)), maxColSize);
            }
            for (int k = 0; k < 3; k++)
            {
                drawRow(builder, columnCount, size, i -> ".", maxColSize);
            }
            for (int k = rows - bottomRows; k < rows; k++)
            {
                final int fk = k;
                drawRow(builder, columnCount, size, i -> Objects.toString(values.get(i).get(fk)), maxColSize);
            }
        }


        drawSeparation(builder, columnCount, size);

        // add summary
        builder.append(rows).append(" rows ").append(rows > maxRowSize ? ("(" + maxRowSize + " shown) ") : "").append("-- ").append(columnCount).append(" columns");
        return builder.toString();
    }

    private static void drawSeparation(StringBuilder builder, int count, MutableList<Integer> size)
    {
        builder.append("+");
        for (int i = 0; i < count; i++)
        {
            repeat('-', size.get(i), builder);
            builder.append("+");
        }
        builder.append("\n");
    }

    private static void repeat(char value, int length, StringBuilder builder)
    {
        for (int k = 0; k < length; k++)
        {
            builder.append(value);
        }
    }

    private static void drawRow(StringBuilder builder, int count, MutableList<Integer> size, Function<Integer, String> getter, int maxColSize)
    {
        builder.append("|");
        for (int i = 0; i < count; i++)
        {
            String val = printValue(getter.apply(i), maxColSize);
            int space = (size.get(i) - val.length()) / 2;
            repeat(' ', space, builder);
            builder.append(val);
            repeat(' ', size.get(i) - val.length() - space, builder);
            builder.append("|");
        }

        builder.append("\n");
    }

    private static String printValue(String str, int max)
    {
        return str.length() > max ? str.substring(0, max - 3) + "..." : str;
    }
}
