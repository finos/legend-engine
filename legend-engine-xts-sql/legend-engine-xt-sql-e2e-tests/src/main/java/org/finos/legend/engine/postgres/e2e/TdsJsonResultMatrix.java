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

import org.finos.legend.engine.postgres.protocol.sql.handler.legend.bridge.LegendColumn;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.bridge.shared.LegendExecutionResultFromTds;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.bridge.shared.LegendTdsResultParser;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.result.LegendResultSet;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapts a raw executePlanAsJSON TDS payload into a typed ResultMatrix, via the same
 * LegendTdsResultParser/LegendResultSet the pg-wire path uses.
 */
public final class TdsJsonResultMatrix
{
    private TdsJsonResultMatrix()
    {
    }

    public static ResultMatrix parse(String tdsJson) throws Exception
    {
        LegendTdsResultParser parser = new LegendTdsResultParser(
                new ByteArrayInputStream(tdsJson.getBytes(StandardCharsets.UTF_8)));
        LegendExecutionResultFromTds executionResult = new LegendExecutionResultFromTds(parser);
        try
        {
            LegendResultSet resultSet = new LegendResultSet(executionResult);
            List<String> columnNames = new ArrayList<>();
            for (LegendColumn c : executionResult.getLegendColumns())
            {
                columnNames.add(c.getName());
            }
            List<List<Object>> rows = new ArrayList<>();
            while (resultSet.next())
            {
                List<Object> row = new ArrayList<>();
                for (int i = 1; i <= columnNames.size(); i++)
                {
                    row.add(resultSet.getObject(i));
                }
                rows.add(row);
            }
            return new ResultMatrix(columnNames, rows);
        }
        finally
        {
            executionResult.close();
        }
    }
}
