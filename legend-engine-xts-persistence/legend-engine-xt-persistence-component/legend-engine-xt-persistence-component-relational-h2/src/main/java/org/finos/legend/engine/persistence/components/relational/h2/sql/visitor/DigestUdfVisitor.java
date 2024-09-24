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

package org.finos.legend.engine.persistence.components.relational.h2.sql.visitor;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.values.*;

import java.util.List;
import java.util.Map;

public class DigestUdfVisitor extends org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.DigestUdfVisitor
{

    protected Value getColumnValueAsStringType(Value value, FieldType dataType, Map<DataType, String> typeConversionUdfNames)
    {
        if (value instanceof StagedFilesFieldValue)
        {
            // The field, being StagedFilesFieldValue, is already a String read from a csv file
            // Hence return a simple FieldValue with just the field name
            return FieldValue.builder().fieldName(((StagedFilesFieldValue)value).fieldName()).build();
        }

        // Else need to convert the field into a String
        if (typeConversionUdfNames.containsKey(dataType.dataType()))
        {
            return org.finos.legend.engine.persistence.components.logicalplan.values.Udf.builder().udfName(typeConversionUdfNames.get(dataType.dataType())).addParameters(value).build();
        }
        else
        {
            return FunctionImpl.builder().functionName(FunctionName.CONVERT).addValue(value, ObjectValue.of(DataType.VARCHAR.name())).build();
        }
    }

    @Override
    protected Value mergeColumnsFunction(List<Value> columns)
    {
        ToArrayFunction toArrayFunction = ToArrayFunction.builder().addAllValues(columns).build();
        return toArrayFunction;
    }
}
