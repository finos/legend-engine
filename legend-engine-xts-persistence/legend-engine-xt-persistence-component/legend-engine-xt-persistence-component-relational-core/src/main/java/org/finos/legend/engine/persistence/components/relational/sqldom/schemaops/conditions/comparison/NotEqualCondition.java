// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.comparison;

import org.finos.legend.engine.persistence.components.relational.sqldom.common.Operator;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Value;

public class NotEqualCondition extends ComparisonCondition
{
    public NotEqualCondition()
    {
        super.operator = Operator.NOT_EQ;
    }

    public NotEqualCondition(Value left, Value right)
    {
        super(left, right, Operator.NOT_EQ);
    }

    @Override
    public void push(Object node)
    {
        if (left == null)
        {
            left = (Value) node;
        }
        else
        {
            right = (Value) node;
        }
    }
}
