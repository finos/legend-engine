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

package org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.values.DigestUdf;
import org.finos.legend.engine.persistence.components.logicalplan.values.StringValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.ToArrayFunction;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Udf;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DigestUdfVisitor implements LogicalPlanVisitor<DigestUdf>
{

    @Override
    public VisitorResult visit(PhysicalPlanNode prev, DigestUdf current, VisitorContext context)
    {
        Udf udf = new Udf(context.quoteIdentifier(), current.udfName());
        prev.push(udf);
        List<Value> columnNameList = new ArrayList<>();
        List<Value> columnValueList = new ArrayList<>();
        for (int i = 0; i < current.values().size(); i++)
        {
            columnNameList.add(StringValue.of(current.fieldNames().get(i)));
            columnValueList.add(getColumnValueAsStringType(current.values().get(i), current.fieldTypes().get(i), current.typeConversionUdfNames()));
        }

        ToArrayFunction toArrayColumnNames = ToArrayFunction.builder().addAllValues(columnNameList).build();
        ToArrayFunction toArrayColumnValues = ToArrayFunction.builder().addAllValues(columnValueList).build();

        return new VisitorResult(udf, Arrays.asList(toArrayColumnNames, toArrayColumnValues));
    }

    protected Value getColumnValueAsStringType(Value value, DataType dataType, Map<DataType, String> typeConversionUdfNames)
    {
        throw new IllegalStateException("UDF is unsupported in ANSI sink");
    }
}
