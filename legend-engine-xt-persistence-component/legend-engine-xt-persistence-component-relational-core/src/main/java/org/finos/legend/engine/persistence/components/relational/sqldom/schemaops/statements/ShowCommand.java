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

package org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.Clause;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.ShowType;

import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.WHITE_SPACE;

public class ShowCommand implements SqlGen
{
    private final ShowType operation;
    private String schemaName;

    public ShowCommand(ShowType operation)
    {
        this(operation, null);
    }

    public ShowCommand(ShowType operation, String schemaName)
    {
        this.operation = operation;
        this.schemaName = schemaName;
    }

    public String getSchemaName()
    {
        return schemaName;
    }

    public void setSchemaName(String schemaName)
    {
        this.schemaName = schemaName;
    }

    /*
    SHOW
   {
   SCHEMAS |
   TABLES [ FROM schemaName ] |
    }
     */

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        builder.append(Clause.SHOW.get());
        builder.append(WHITE_SPACE + operation.name());
        if (operation == ShowType.TABLES && schemaName != null)
        {
            builder.append(WHITE_SPACE + Clause.FROM.get());
            builder.append(WHITE_SPACE + schemaName);
        }
    }
}
