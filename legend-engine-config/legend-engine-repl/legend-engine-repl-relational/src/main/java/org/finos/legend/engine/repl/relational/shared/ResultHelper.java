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

package org.finos.legend.engine.repl.relational.shared;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;

import java.sql.ResultSet;
import java.util.List;

public class ResultHelper
{
    public static String prettyGridPrint(ResultSet resultSet, List<String> columnNames, List<String> columnTypes, int maxRowSize, int maxColSize)
    {
        MutableList<String> columns = Lists.mutable.empty();
        MutableList<Integer> size = Lists.mutable.empty();
        MutableList<MutableList<String>> values = Lists.mutable.empty();

        try
        {
            int columnCount = columnNames.size();

            // collect data
            for (int i = 0; i < columnCount; i++)
            {
                columns.add(columnNames.get(i));
                values.add(Lists.mutable.empty());
            }
            while (resultSet.next())
            {
                for (int i = 1; i <= columnCount; i++)
                {
                    String value = resultSet.getObject(i) == null ? "" : resultSet.getObject(i).toString();
                    values.get(i - 1).add(value);
                }
            }

            // determine the max size for each column
            for (int i = 0; i < columnCount; i++)
            {
                size.add(values.get(i).injectInto(Math.max(columns.get(i).length(), columnTypes.get(i).length()), (IntObjectToIntFunction<? super String>) (a, b) -> Math.max(b.length(), a)));
            }
            size = Lists.mutable.withAll(size.collect(s -> Math.min(maxColSize, s + 2)));

            // print the result
            StringBuilder builder = new StringBuilder();

            drawSeparation(builder, columnCount, size);
            drawRow(builder, columnCount, size, columns::get, maxColSize);
            drawRow(builder, columnCount, size, columnTypes::get, maxColSize);
            drawSeparation(builder, columnCount, size);

            int rows = values.get(0).size();
            if (rows <= maxRowSize)
            {
                for (int k = 0; k < rows; k++)
                {
                    final int fk = k;
                    drawRow(builder, columnCount, size, i -> values.get(i).get(fk), maxColSize);
                }
            }
            else
            {
                int topRows = (int) Math.ceil((float) maxRowSize / 2);
                int bottomRows = maxRowSize - topRows;
                for (int k = 0; k < topRows; k++)
                {
                    final int fk = k;
                    drawRow(builder, columnCount, size, i -> values.get(i).get(fk), maxColSize);
                }
                for (int k = 0; k < 3; k++)
                {
                    drawRow(builder, columnCount, size, i -> ".", maxColSize);
                }
                for (int k = rows - bottomRows; k < rows; k++)
                {
                    final int fk = k;
                    drawRow(builder, columnCount, size, i -> values.get(i).get(fk), maxColSize);
                }
            }


            drawSeparation(builder, columnCount, size);

            // add summary
            builder.append(rows + " rows " + (rows > maxRowSize ? ("(" + maxRowSize + " shown) ") : "") + "-- " + columns.size() + " columns");
            return builder.toString();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
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
        return str.length() >= max ? str.substring(0, max - 3 - 2) + "..." : str;
    }
}
