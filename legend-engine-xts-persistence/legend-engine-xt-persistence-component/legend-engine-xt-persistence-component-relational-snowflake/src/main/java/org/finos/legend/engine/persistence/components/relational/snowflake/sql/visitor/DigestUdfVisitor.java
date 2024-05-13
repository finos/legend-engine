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

package org.finos.legend.engine.persistence.components.relational.snowflake.sql.visitor;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.values.CastFunction;
import org.finos.legend.engine.persistence.components.logicalplan.values.StagedFilesFieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;

import java.util.Map;

public class DigestUdfVisitor extends org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.DigestUdfVisitor
{
    protected Value getColumnValueAsStringType(Value value, FieldType dataType, Map<DataType, String> typeConversionUdfNames)
    {
        if (value instanceof StagedFilesFieldValue)
        {
            if (typeConversionUdfNames.containsKey(dataType.dataType()))
            {
                // TO_STRING(CAST(field))
                return org.finos.legend.engine.persistence.components.logicalplan.values.Udf.builder().udfName(typeConversionUdfNames.get(dataType.dataType())).addParameters(CastFunction.builder().field(value).type(dataType).build()).build();
            }
            else
            {
                // CAST(field)
                return CastFunction.builder().field(value).type(dataType).build();
            }
        }
        else
        {
            if (typeConversionUdfNames.containsKey(dataType.dataType()))
            {
                // TO_STRING(field)
                return org.finos.legend.engine.persistence.components.logicalplan.values.Udf.builder().udfName(typeConversionUdfNames.get(dataType.dataType())).addParameters(value).build();
            }
            else
            {
                // field
                return value;
            }
        }
    }
}
