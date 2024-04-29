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

package org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.schemaops.expressions.table;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.TableLike;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.StringUtils;

public class StagedFilesTable extends TableLike
{
    private String location;

    public StagedFilesTable(String location)
    {
        this.location = location;
    }

    public StagedFilesTable(String alias, String location)
    {
        super(alias);
        this.location = location;
    }

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        validate();
        genSqlWithoutAlias(builder);
        super.genSql(builder);
    }

    @Override
    public void genSqlWithoutAlias(StringBuilder builder) throws SqlDomException
    {
        validate();
        builder.append(location);
    }

    @Override
    public void push(Object node)
    {

    }

    void validate() throws SqlDomException
    {
        if (StringUtils.empty(location))
        {
            throw new SqlDomException("location is mandatory");
        }
    }
}
