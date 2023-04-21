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

package org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.Order;

import java.util.Optional;

import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.WHITE_SPACE;

public class OrderedField extends Field
{
    private Optional<Order> order;

    public OrderedField(String name, String quoteIdentifier, Optional<Order> order)
    {
        super(name, quoteIdentifier);
        this.order = order;
    }

    public OrderedField(String datasetReferenceAlias, String name, String quoteIdentifier, String alias, Optional<Order> order)
    {
        super(datasetReferenceAlias, name, quoteIdentifier, alias);
        this.order = order;
    }

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        validate();
        super.genSqlWithoutAlias(builder);
        if (order.isPresent())
        {
            builder.append(WHITE_SPACE + order.get());
        }
    }
}
