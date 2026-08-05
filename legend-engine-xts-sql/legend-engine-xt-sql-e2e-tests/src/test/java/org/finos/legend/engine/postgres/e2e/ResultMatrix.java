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

package org.finos.legend.engine.postgres.e2e;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Holds a typed result grid from a SQL query execution.
 */
public class ResultMatrix
{
    private final List<String> columnNames;
    private final List<List<Object>> rows;

    public ResultMatrix(List<String> columnNames, List<List<Object>> rows)
    {
        this.columnNames = columnNames;
        this.rows = rows;
    }

    public List<String> getColumnNames()
    {
        return columnNames;
    }

    public List<List<Object>> getRows()
    {
        return rows;
    }

    public int getRowCount()
    {
        return rows.size();
    }

    public int getColumnCount()
    {
        return columnNames.size();
    }

    /**
     * Returns a sorted copy of this matrix (sorts rows by all columns, numeric-aware).
     * Used when query has no ORDER BY.
     */
    public ResultMatrix sorted()
    {
        List<List<Object>> sortedRows = new ArrayList<>(rows);
        sortedRows.sort((a, b) ->
        {
            for (int i = 0; i < a.size(); i++)
            {
                Object av = a.get(i);
                Object bv = b.get(i);
                if (av == null && bv == null)
                {
                    continue;
                }
                if (av == null)
                {
                    return -1;
                }
                if (bv == null)
                {
                    return 1;
                }
                int cmp;
                if (av instanceof Number && bv instanceof Number)
                {
                    cmp = Double.compare(((Number) av).doubleValue(), ((Number) bv).doubleValue());
                }
                else
                {
                    cmp = av.toString().compareTo(bv.toString());
                }
                if (cmp != 0)
                {
                    return cmp;
                }
            }
            return 0;
        });
        return new ResultMatrix(columnNames, sortedRows);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(" | ", columnNames)).append("\n");
        sb.append(String.join("", Collections.nCopies(columnNames.size() * 15, "-"))).append("\n");
        for (List<Object> row : rows)
        {
            sb.append(row.stream().map(o -> o == null ? "NULL" : o.toString()).collect(Collectors.joining(" | "))).append("\n");
        }
        return sb.toString();
    }
}
