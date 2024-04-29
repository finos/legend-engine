//  Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.entitlement.model.specification;

public class RelationalDatabaseTableSpecification extends DatasetSpecification
{
    private String database;
    private String schema;
    private String table;

    public RelationalDatabaseTableSpecification(String name, String type, String database, String schema, String table)
    {
        super(name, type);
        this.database = database;
        this.schema = schema;
        this.table = table;
    }

    public RelationalDatabaseTableSpecification()
    {
        // DO NOT DELETE: this resets the default constructor for Jackson
    }

    public String getDatabase()
    {
        return this.database;
    }

    public String getTable()
    {
        return this.table;
    }

    public String getSchema()
    {
        return this.schema;
    }
}
