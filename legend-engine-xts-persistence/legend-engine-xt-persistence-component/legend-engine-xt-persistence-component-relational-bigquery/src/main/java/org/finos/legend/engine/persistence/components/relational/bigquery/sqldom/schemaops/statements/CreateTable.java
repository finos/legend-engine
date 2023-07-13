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

package org.finos.legend.engine.persistence.components.relational.bigquery.sqldom.schemaops.statements;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.Clause;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.table.ClusteringKeyConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.table.PartitionKeyConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.table.TableConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.modifiers.TableModifier;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.Column;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.Table;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.DDLStatement;
import org.finos.legend.engine.persistence.components.relational.sqldom.tabletypes.TableType;

import java.util.ArrayList;
import java.util.List;

import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.CLOSING_PARENTHESIS;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.COMMA;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.EMPTY;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.OPEN_PARENTHESIS;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.WHITE_SPACE;

public class CreateTable implements DDLStatement
{
    private Table table;
    private final List<TableModifier> modifiers; // dataset
    private final List<Column> columns; // schema
    private final List<TableConstraint> tableConstraints; // table level
    private final List<TableType> types; // dataset
    private final List<ClusteringKeyConstraint> clusterKeys;
    private final List<PartitionKeyConstraint> partitionKeys;

    public CreateTable()
    {
        this.modifiers = new ArrayList<>();
        this.columns = new ArrayList<>();
        this.tableConstraints = new ArrayList<>();
        this.types = new ArrayList<>();
        this.clusterKeys = new ArrayList<>();
        this.partitionKeys = new ArrayList<>();
    }

    /*
     CREATE GENERIC PLAN:
     CREATE [TABLE TYPE] TABLE [IF NOT EXISTS] {FULLY_QUALIFIED_TABLE_NAME}( {COLUMNS}{CONSTRAINTS} )
     PARTITION BY <>
     CLUSTER BY <>
     */
    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        validate();
        builder.append(Clause.CREATE.get());

        // Table Type
        SqlGen.genSqlList(builder, types, WHITE_SPACE, WHITE_SPACE);

        builder.append(WHITE_SPACE + Clause.TABLE.get());

        // Modifiers
        SqlGen.genSqlList(builder, modifiers, WHITE_SPACE, WHITE_SPACE);

        // Table name
        builder.append(WHITE_SPACE);
        table.genSqlWithoutAlias(builder);

        // Columns + table constraints
        builder.append(OPEN_PARENTHESIS);
        SqlGen.genSqlList(builder, columns, EMPTY, COMMA);
        SqlGen.genSqlList(builder, tableConstraints, COMMA, COMMA);
        builder.append(CLOSING_PARENTHESIS);

        // Partition Key Expression
        if (!partitionKeys.isEmpty())
        {
            builder.append(WHITE_SPACE + Clause.PARTITION_BY.get() + WHITE_SPACE);
            SqlGen.genSqlList(builder, partitionKeys, EMPTY, COMMA);
        }

        // Clustering keys
        if (!clusterKeys.isEmpty())
        {
            builder.append(WHITE_SPACE + Clause.CLUSTER_BY.get() + WHITE_SPACE);
            SqlGen.genSqlList(builder, clusterKeys, EMPTY, COMMA);
        }
    }


    @Override
    public void push(Object node)
    {
        if (node instanceof Table)
        {
            table = (Table) node;
        }
        else if (node instanceof TableType)
        {
            types.add((TableType) node);
        }
        else if (node instanceof TableModifier)
        {
            modifiers.add((TableModifier) node);
        }
        else if (node instanceof Column)
        {
            columns.add((Column) node);
        }
        else if (node instanceof TableConstraint)
        {
            tableConstraints.add((TableConstraint) node);
        }
        else if (node instanceof ClusteringKeyConstraint)
        {
            clusterKeys.add((ClusteringKeyConstraint) node);
        }
        else if (node instanceof PartitionKeyConstraint)
        {
            partitionKeys.add((PartitionKeyConstraint) node);
        }
    }

    void validate() throws SqlDomException
    {
        if (table == null)
        {
            throw new SqlDomException("Table is mandatory for Create Table Command");
        }
        if (columns == null || columns.isEmpty())
        {
            throw new SqlDomException("Columns list is mandatory for Create Table Command");
        }
        if (!partitionKeys.isEmpty() && partitionKeys.size() != 1)
        {
            throw new SqlDomException("Only one Partition Key expression is allowed for BigQuery Create Table Command");
        }
    }
}
