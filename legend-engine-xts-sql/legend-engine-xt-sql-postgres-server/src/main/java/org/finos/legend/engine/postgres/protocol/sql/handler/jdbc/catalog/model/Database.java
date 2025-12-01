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

public class Database
{
    static int _dbId = 20000;

    private final String name;

    private final MutableList<Schema> schemas;

    private final int dbId;

    public Database(String name)
    {
        this.name = name;
        this.schemas = Lists.mutable.empty();
        this.dbId = _dbId++;
    }

    public String getName()
    {
        return this.name;
    }

    public Schema schema(Schema schema)
    {
        this.schemas.add(schema);
        schema.setDatabase(this);
        return schema;
    }

    public MutableList<Schema> getSchemas()
    {
        return schemas;
    }

    public int getDbId()
    {
        return this.dbId;
    }
}
