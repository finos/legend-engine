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

package org.finos.legend.engine.persistence.components.relational.h2.sqldom.schemaops.values;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.FunctionName;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.HashAlgorithm;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Function;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.StringValue;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HashFunction extends Value
{
    private HashAlgorithm algorithm;
    private List<Value> values;

    public HashFunction(HashAlgorithm hashAlgorithm)
    {
        this.algorithm = hashAlgorithm;
        this.values = new ArrayList<>();
    }

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        genSqlWithoutAlias(builder);
        super.genSql(builder);
    }

    // H2 hash function format : RAWTOHEX(HASH(algorithm,CONCAT(columns)))
    @Override
    public void genSqlWithoutAlias(StringBuilder builder) throws SqlDomException
    {
        Function concatFunction = new Function(FunctionName.CONCAT, values);
        Function hashFunction = new Function(FunctionName.HASH, Arrays.asList(
            new StringValue(algorithm.name()), concatFunction));
        Function md5Function = new Function(FunctionName.RAW_TO_HEX, Arrays.asList(hashFunction));
        md5Function.genSqlWithoutAlias(builder);
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