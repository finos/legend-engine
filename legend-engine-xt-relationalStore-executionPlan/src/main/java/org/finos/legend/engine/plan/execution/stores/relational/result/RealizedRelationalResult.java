// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.result;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.plan.execution.result.ExecutionActivity;
import org.finos.legend.engine.plan.execution.result.ResultVisitor;
import org.finos.legend.engine.plan.execution.result.StreamingResult;
import org.finos.legend.engine.plan.execution.result.builder.Builder;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.result.serialization.Serializer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.result.SQLResultColumn;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class RealizedRelationalResult extends StreamingResult
{
    public Builder builder;
    public List<ExecutionActivity> activities;
    public List<SQLResultColumn> columns;
    public List<List<Object>> resultSetRows;
    public List<List<Object>> transformedRows;

    private static final int DEFAULT_ROW_LIMIT = 1000;
    public static final String ROW_LIMIT_PROPERTY_NAME = "org.finos.legend.engine.realizedRelationalResultRowLimit";

    public RealizedRelationalResult(RelationalResult relationalResult) throws SQLException
    {
        super(relationalResult.activities);
        this.builder = relationalResult.builder;
        this.columns = relationalResult.getSQLResultColumns();
        int columnCount = this.columns.size();

        this.transformedRows = Lists.mutable.empty();
        this.resultSetRows = Lists.mutable.empty();
        ResultSet resultSet = relationalResult.resultSet;
        int SUPPORTED_RESULT_ROWS = getRowLimit();
        int rowCount = 0;
        try
        {
            while (resultSet.next())
            {
                if (rowCount > SUPPORTED_RESULT_ROWS)
                {
                    throw new RuntimeException("Too many rows returned. Realization of relational results currently supports results with up to " + SUPPORTED_RESULT_ROWS + " rows.");
                }

                List<Object> transformedRow = Lists.mutable.empty();
                List<Object> resultSetRow = Lists.mutable.empty();
                MutableList<Function<Object, Object>> transformers = relationalResult.getTransformers();
                for (int i = 1; i <= columnCount - 1; i++)
                {
                    transformedRow.add(transformers.get(i - 1).valueOf(relationalResult.getValue(i)));
                    resultSetRow.add(relationalResult.getValue(i));
                }
                transformedRow.add(transformers.get(columnCount - 1).valueOf(relationalResult.getValue(columnCount)));
                resultSetRow.add(relationalResult.getValue(columnCount));
                transformedRows.add(transformedRow);
                resultSetRows.add(resultSetRow);
                rowCount += 1;
            }
        }
        finally
        {
            relationalResult.close();
        }
    }

    public int getRowLimit()
    {
        return Integer.getInteger(ROW_LIMIT_PROPERTY_NAME, DEFAULT_ROW_LIMIT);
    }

    private RealizedRelationalResult()
    {
        super(Lists.mutable.empty());
    }

    @Override
    public <T> T accept(ResultVisitor<T> resultVisitor)
    {
        return ((RelationalResultVisitor<T>) resultVisitor).visit(this);
    }


    public static RealizedRelationalResult emptyRealizedRelationalResult(List<SQLResultColumn> resultColumns)
    {
        RealizedRelationalResult realizedRelationalResult = new RealizedRelationalResult();
        realizedRelationalResult.columns = resultColumns;
        realizedRelationalResult.transformedRows = Lists.mutable.empty();
        realizedRelationalResult.resultSetRows = Lists.mutable.empty();

        return realizedRelationalResult;
    }

    public void addRow(List<Object> resultSetRow, List<Object> transformedRow)
    {
        this.resultSetRows.add(resultSetRow);
        this.transformedRows.add(transformedRow);
    }

    public List<Map<String, Object>> getRowValueMaps(boolean withTransform)
    {
        List<Map<String, Object>> rowValueMaps = Lists.mutable.empty();
        (withTransform ? this.transformedRows : this.resultSetRows).forEach(row ->
        {
            Map<String, Object> rowValMap = Maps.mutable.empty();
            int index = 0;
            for (SQLResultColumn col : this.columns)
            {
                rowValMap.put(col.getNonQuotedLabel(), row.get(index));
                index += 1;
            }
            rowValueMaps.add(rowValMap);
        });
        return rowValueMaps;
    }

    @Override
    public Builder getResultBuilder()
    {
        return this.builder;
    }

    @Override
    public Serializer getSerializer(SerializationFormat format)
    {
        throw new UnsupportedOperationException("Not yet implemented!");
    }
}