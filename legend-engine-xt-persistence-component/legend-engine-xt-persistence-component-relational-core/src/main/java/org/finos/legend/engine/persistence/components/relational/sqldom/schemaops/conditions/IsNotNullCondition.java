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

package org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Value;

public class IsNotNullCondition extends Condition
{
    private final Value value;

    public IsNotNullCondition(Value value)
    {
        this.value = value;
    }

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        validate();
        value.genSqlWithoutAlias(builder);
        builder.append(" IS NOT NULL");
    }

    void validate() throws SqlDomException
    {
        if (value == null)
        {
            throw new SqlDomException("value is null");
        }
    }
}
