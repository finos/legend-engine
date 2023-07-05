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
import org.finos.legend.engine.persistence.components.relational.sqldom.common.FunctionName;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.HashAlgorithm;

import java.util.ArrayList;
import java.util.List;

import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.CLOSING_PARENTHESIS;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.OPEN_PARENTHESIS;

public class HashFunction extends Value
{
    private HashAlgorithm algorithm;
    private List<Value> values;

    public HashFunction(HashAlgorithm algorithm, String quoteIdentifier)
    {
        super(quoteIdentifier);
        this.algorithm = algorithm;
        this.values = new ArrayList<>();
    }

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        genSqlWithoutAlias(builder);
        super.genSql(builder);
    }

    @Override
    public void genSqlWithoutAlias(StringBuilder builder) throws SqlDomException
    {
        Function concatFunction = new Function(FunctionName.CONCAT, values, getQuoteIdentifier());
        builder.append(algorithm.name());
        builder.append(OPEN_PARENTHESIS);
        concatFunction.genSqlWithoutAlias(builder);
        builder.append(CLOSING_PARENTHESIS);
    }

    @Override
    public void push(Object node)
    {
        if (node instanceof HashAlgorithm)
        {
            algorithm = (HashAlgorithm) node;
        }
        else if (node instanceof Value)
        {
            values.add((Value) node);
        }
    }
}
