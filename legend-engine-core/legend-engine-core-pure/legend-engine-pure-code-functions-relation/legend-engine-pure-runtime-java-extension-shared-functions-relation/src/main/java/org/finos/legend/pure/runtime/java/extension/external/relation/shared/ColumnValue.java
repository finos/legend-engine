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

package org.finos.legend.pure.runtime.java.extension.external.relation.shared;

import io.deephaven.csv.parsers.DataType;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

public class ColumnValue
{
    String name;
    DataType type;
    Object result;
    boolean[] nulls = null;

    public ColumnValue(String name, DataType type, Object result)
    {
        this.name = name;
        this.type = type;
        this.result = result;
    }

    public ColumnValue(String name, DataType type, Object result, boolean[] nulls)
    {
        this.name = name;
        this.type = type;
        this.result = result;
        this.nulls = nulls;
    }

    @Override
    public String toString()
    {
        MutableList<String> res;
        if (type == DataType.INT)
        {
            int[] val = (int[]) result;
            res = extracted(i -> String.valueOf(val[i]), val.length);
        }
        else if (type == DataType.STRING)
        {
            String[] val = (String[]) result;
            res = extracted(i -> val[i], val.length);
        }
        else if (type == DataType.DOUBLE)
        {
            double[] val = (double[]) result;
            res = extracted(i -> String.valueOf(val[i]), val.length);
        }
        else
        {
            throw new RuntimeException("TODO");
        }

        return "ColumnValue{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", result=" + res.makeString("[", ",", "]") +
                '}';
    }

    private MutableList<String> extracted(Function<Integer, String> function, int size)
    {
        MutableList<String> res = Lists.mutable.empty();
        for (int i = 0; i < size; i++)
        {
            res.add(function.valueOf(i));
        }
        return res;
    }
}
