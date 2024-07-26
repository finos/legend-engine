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

import static org.jline.jansi.Ansi.ansi;

public class ResultHelper
{
    // TODO: the return of this will be printed directly to the console, so we should be mindful of the size
    // in order to not flood the console, and making client wait for the print to finish before moving on to next operation
    public static String prettyGridPrint(ResultSet resultSet, List<String> columnNames, List<String> headers, int maxRowSize, int maxColSize)
    {

        MutableList<String> columns = Lists.mutable.empty();
        MutableList<Integer> size = Lists.mutable.empty();
        MutableList<MutableList<String>> values = Lists.mutable.empty();

        try
        {
            int columnCount = columnNames.size();
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
            for (int i = 0; i < columnCount; i++)
            {
                size.add(values.get(i).injectInto(columns.get(i).length(), (IntObjectToIntFunction<? super String>) (a, b) -> Math.max(b.length(), a)));
            }
            size = Lists.mutable.withAll(size.collect(s -> Math.min(maxColSize, s + 2)));

            StringBuilder builder = new StringBuilder();

            drawSeparation(builder, columnCount, size);
            drawRow(builder, columnCount, size, columns::get, maxColSize);
            drawSeparation(builder, columnCount, size);

            int rows = values.get(0).size();
            for (int k = 0; k < rows; k++)
            {
                final int fk = k;
                drawRow(builder, columnCount, size, i -> values.get(i).get(fk), maxColSize);
            }

            drawSeparation(builder, columnCount, size);

            // add summary
            builder.append(ansi().fgBrightBlack().a(rows + " rows -- " + columns.size() + " columns").reset());
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
