// Copyright 2025 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.postgres.protocol.sql.handler.jdbc.catalog.model;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

public class Schema
{
    static int _schemaId = 20000;

    private final String name;

    private Database database;

    private final int schemaId;

    private final MutableList<Table> tables;

    private final MutableList<Function> functions;

    public Schema(String name)
    {
        this.name = name;
        this.tables = Lists.mutable.empty();
        this.functions = Lists.mutable.empty();
        this.schemaId = _schemaId++;

    }

    public String getName()
    {
        return this.name;
    }

    public Table table(Table table)
    {
        this.tables.add(table);
        table.setSchema(this);
        return table;
    }

    public Function function(Function function)
    {
        this.functions.add(function);
        function.setSchema(this);
        return function;
    }


    public MutableList<Table> getTables()
    {
        return this.tables;
    }

    public MutableList<Function> getFunctions()
    {
        return this.functions;
    }

    public void setDatabase(Database database)
    {
        this.database = database;
    }

    public int getSchemaId()
    {
        return this.schemaId;
    }

    public Database getDatabase()
    {
        return this.database;
    }
}
