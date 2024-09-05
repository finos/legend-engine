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
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
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
        List<Value> columns = new ArrayList<>();
        for (int i = 0; i < current.values().size(); i++)
        {
            Value columnName = StringValue.of(current.fieldNames().get(i));
            Value columnValue = getColumnValueAsStringType(current.values().get(i), current.fieldTypes().get(i), current.typeConversionUdfNames());
            if (current.columnTransformationUdfName().isPresent())
            {
                columns.add(org.finos.legend.engine.persistence.components.logicalplan.values.Udf.builder().udfName(current.columnTransformationUdfName().get()).addParameters(columnName, columnValue).build());
            }
            else
            {
                columns.add(columnName);
                columns.add(columnValue);
            }
        }

        ToArrayFunction toArrayFunction = ToArrayFunction.builder().addAllValues(columns).build();

        return new VisitorResult(udf, Arrays.asList(toArrayFunction));
    }

    protected Value getColumnValueAsStringType(Value value, FieldType dataType, Map<DataType, String> typeConversionUdfNames)
    {
        throw new IllegalStateException("UDF is unsupported in ANSI sink");
    }
}
