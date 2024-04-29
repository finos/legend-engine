// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.repl.relational.grid;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;

import java.sql.ResultSet;

public class Grid
{
    public static String prettyGridPrint(RelationalResult res, int maxColSize)
    {
        MutableList<String> columns = Lists.mutable.empty();
        MutableList<Integer> size = Lists.mutable.empty();
        MutableList<MutableList<String>> values = Lists.mutable.empty();

        try (ResultSet rs = res.resultSet)
        {
            int count = res.sqlColumns.size();
            for (int i = 0; i < count; i++)
            {
                columns.add(res.sqlColumns.get(i));
                values.add(Lists.mutable.empty());
            }
            while (rs.next())
            {
                for (int i = 1; i <= count; i++)
                {
                    values.get(i - 1).add(rs.getObject(i) == null ? "" : rs.getObject(i).toString());
                }
            }
            for (int i = 0; i < count; i++)
            {
                size.add(values.get(i).injectInto(columns.get(i).length(), (IntObjectToIntFunction<? super String>) (a, b) -> Math.max(b.length(), a)));
            }
            size = Lists.mutable.withAll(size.collect(s -> Math.min(maxColSize, s + 2)));

            StringBuilder builder = new StringBuilder();

            drawSeparation(builder, count, size);
            drawRow(builder, count, size, columns::get, maxColSize);
            drawSeparation(builder, count, size);

            int rows = values.get(0).size();
            for (int k = 0; k < rows; k++)
            {
                final int fk = k;
                drawRow(builder, count, size, i -> values.get(i).get(fk), maxColSize);
            }

            drawSeparation(builder, count, size);


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
