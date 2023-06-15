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

package org.finos.legend.engine.persistence.components.common;

import org.finos.legend.engine.persistence.components.logicalplan.conditions.GreaterThan;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Condition;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.GreaterThanEqualTo;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.LessThan;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.LessThanEqualTo;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Equals;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.ObjectValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.StringValue;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(
        typeAbstract = "*Abstract",
        typeImmutable = "*",
        jdkOnly = true,
        optionalAcceptNullable = true,
        strictBuilder = true
)
public interface DatasetFilterAbstract
{
    @Value.Parameter(order = 0)
    String fieldName();

    @Value.Parameter(order = 1)
    FilterType filterType();

    @Value.Parameter(order = 2)
    Object value();

    default Condition mapFilterToCondition(DatasetReference datasetReference)
    {
        FieldValue fieldValue = FieldValue.builder().fieldName(fieldName()).datasetRef(datasetReference).build();
        org.finos.legend.engine.persistence.components.logicalplan.values.Value value;
        if (value() instanceof Number)
        {
            value = ObjectValue.of(value());
        }
        else
        {
            value = StringValue.of(value().toString());
        }

        switch (filterType())
        {
            case GREATER_THAN:
                return GreaterThan.of(fieldValue, value);
            case GREATER_THAN_EQUAL:
                return GreaterThanEqualTo.of(fieldValue, value);
            case LESS_THAN:
                return LessThan.of(fieldValue, value);
            case LESS_THAN_EQUAL:
                return LessThanEqualTo.of(fieldValue, value);
            case EQUAL_TO:
                return Equals.of(fieldValue, value);
            default:
                throw new IllegalStateException("Unsupported Filter Type");
        }
    }

    default Object getValue()
    {
        Object val = value();
        if (!(value() instanceof Number))
        {
            val = String.valueOf(value());
        }
        return val;
    }

}
