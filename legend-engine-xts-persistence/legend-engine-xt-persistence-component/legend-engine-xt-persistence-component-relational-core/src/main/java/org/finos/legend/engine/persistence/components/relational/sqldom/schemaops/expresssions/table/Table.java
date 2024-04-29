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

package org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.StringUtils;

public class Table extends TableLike
{
    private String db;
    private String schema;
    private String table;
    private String quoteIdentifier;

    public Table()
    {
    }

    public Table(String db, String schema, String table, String alias, String quoteIdentifier)
    {
        super(alias);
        this.db = db;
        this.schema = schema;
        this.table = table;
        this.quoteIdentifier = quoteIdentifier;
    }

    public String getDb()
    {
        return db;
    }

    public void setDb(String db)
    {
        this.db = db;
    }

    public String getSchema()
    {
        return schema;
    }

    public void setSchema(String schema)
    {
        this.schema = schema;
    }

    public String getTable()
    {
        return table;
    }

    public void setTable(String table)
    {
        this.table = table;
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
        /*
         To produce :
         Case 1: db and schema both present: "<db>"."<schema>"."<table>"
         Case 2: If only db present: "<db>"."<table>"
         Case 3: If only schema present: "<schema>"."<table>"
         Case 4: If only table present: <table>
         */

        if (StringUtils.notEmpty(db) || StringUtils.notEmpty(schema))
        {
            if (StringUtils.notEmpty(db))
            {
                builder.append(String.format("%s.", SqlGenUtils.getQuotedField(db, quoteIdentifier)));
            }
            if (StringUtils.notEmpty(schema))
            {
                builder.append(String.format("%s.", SqlGenUtils.getQuotedField(schema, quoteIdentifier)));
            }
            builder.append(SqlGenUtils.getQuotedField(table, quoteIdentifier));
        }
        else
        {
            builder.append(table);
        }
    }

    @Override
    public void push(Object node)
    {
        if (db == null)
        {
            db = (String) node;
        }
        else
        {
            table = (String) node;
        }
    }

    void validate() throws SqlDomException
    {
        if (StringUtils.empty(table))
        {
            throw new SqlDomException("Table name is mandatory");
        }
    }
}
