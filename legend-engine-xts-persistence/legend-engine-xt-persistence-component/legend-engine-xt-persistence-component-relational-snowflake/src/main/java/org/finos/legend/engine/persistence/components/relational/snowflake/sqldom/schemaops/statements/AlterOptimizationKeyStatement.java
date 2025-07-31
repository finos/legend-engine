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

package org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.schemaops.statements;

import org.finos.legend.engine.persistence.components.logicalplan.operations.AlterOptimizationKeyAbstract;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.Clause;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.table.ClusteringKeyConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.Table;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.DDLStatement;

import java.util.ArrayList;
import java.util.List;

import static org.finos.legend.engine.persistence.components.relational.sqldom.common.Clause.DROP;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.*;

public class AlterOptimizationKeyStatement implements DDLStatement
{
    private final AlterOptimizationKeyAbstract.AlterOperation operation;
    private Table table;
    private final List<ClusteringKeyConstraint> clusterKeys;

    public AlterOptimizationKeyStatement(AlterOptimizationKeyAbstract.AlterOperation operation)
    {
        this.operation = operation;
        this.clusterKeys = new ArrayList<>();
    }

    public Table getTable()
    {
        return table;
    }

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        validate();
        builder.append(Clause.ALTER.get());

        builder.append(WHITE_SPACE + Clause.TABLE.get());

        builder.append(WHITE_SPACE);
        table.genSqlWithoutAlias(builder);

        builder.append(WHITE_SPACE);

        switch (operation)
        {
            case ALTER_CLUSTER_KEY:
                if (clusterKeys.isEmpty())
                {
                    builder.append(WHITE_SPACE + DROP.get() + WHITE_SPACE + "CLUSTERING KEY");
                }
                else
                {
                    builder.append(WHITE_SPACE + Clause.CLUSTER_BY.get() + WHITE_SPACE);
                    builder.append(OPEN_PARENTHESIS);
                    SqlGen.genSqlList(builder, clusterKeys, WHITE_SPACE, COMMA);
                    builder.append(CLOSING_PARENTHESIS);
                }
                break;
            default:
                throw new SqlDomException("Alter operation " + operation.name() + " not supported");
        }
    }

    @Override
    public void push(Object node)
    {
        if (node instanceof Table)
        {
            table = (Table) node;
        }
        else if (node instanceof ClusteringKeyConstraint)
        {
            clusterKeys.add((ClusteringKeyConstraint) node);
        }
    }

    void validate() throws SqlDomException
    {
        if (table == null)
        {
            throw new SqlDomException("Table is mandatory for Alter Table Command");
        }
    }
}
