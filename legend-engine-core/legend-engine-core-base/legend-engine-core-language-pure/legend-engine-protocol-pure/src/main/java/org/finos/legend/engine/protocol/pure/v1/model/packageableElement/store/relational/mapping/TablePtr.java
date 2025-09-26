// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;

import java.util.Objects;

public class TablePtr
{
    public String _type;
    public String table;
    public String schema;
    public String database;
    public String mainTableDb;
    public SourceInformation sourceInformation;

    @JsonIgnore
    @BsonIgnore
    public String getDb()
    {
        return mainTableDb == null ? database : mainTableDb;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        TablePtr tablePtr = (TablePtr) o;
        return Objects.equals(_type, tablePtr._type) &&
                Objects.equals(table, tablePtr.table) &&
                Objects.equals(schema, tablePtr.schema) &&
                Objects.equals(database, tablePtr.database);
    }

    @Override
    public int hashCode()
    {
        // Use Objects.hash() to generate a hash code from the same fields used in equals()
        return Objects.hash(_type, table, schema, database);
    }
}
