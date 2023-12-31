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

package org.finos.legend.engine.repl;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

public class Grid
{
    public static String prettyGridPrint(RelationalResult res)
    {
        MutableList<String> columns = Lists.mutable.empty();
        MutableList<Integer> size = Lists.mutable.empty();
        MutableList<MutableList<String>> values = Lists.mutable.empty();
        try (ResultSet rs = res.resultSet)
        {
            ResultSetMetaData md = rs.getMetaData();
            int count = md.getColumnCount();
            for (int i = 1; i <= count; i++)
            {
                columns.add(md.getColumnName(i));
                values.add(Lists.mutable.empty());
            }
            while (rs.next())
            {
                for (int i = 1; i <= count; i++)
                {
                    values.get(i - 1).add(rs.getObject(i) == null ? "" : rs.getObject(i).toString());
                }
            }
            for (int i = 1; i <= count; i++)
            {
                int maxSize = columns.get(i - 1).length();
                size.add(values.get(i - 1).injectInto(maxSize, (IntObjectToIntFunction<? super String>) (a, b) -> Math.max(b.length(), a)));
            }
            size = Lists.mutable.withAll(size.collect(s -> s + 2));

            StringBuilder builder = new StringBuilder();

            drawSeparation(builder, count, size);
            drawRow(builder, count, size, columns::get);
            drawSeparation(builder, count, size);

            int rows = values.get(0).size();
            for (int k = 0; k < rows; k++)
            {
                final int fk = k;
                drawRow(builder, count, size, i -> values.get(i).get(fk));
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

    private static void drawRow(StringBuilder builder, int count, MutableList<Integer> size, Function<Integer, String> getter)
    {
        builder.append("|");
        for (int i = 0; i < count; i++)
        {
            String val = getter.apply(i);
            int space = (size.get(i) - val.length()) / 2;
            repeat(' ', space, builder);
            builder.append(val);
            repeat(' ', size.get(i) - val.length() - space, builder);
            builder.append("|");
        }

        builder.append("\n");
    }
}
