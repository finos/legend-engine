// Copyright 2023 Goldman Sachs
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
//

package org.finos.legend.engine.query.sql.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class SchemaColumn
{
    private final String name;
    private final String type;

    public SchemaColumn(@JsonProperty("name") String name, @JsonProperty("type") String type)
    {
        this.name = name;
        this.type = type;
    }

    public String getName()
    {
        return name;
    }

    public String getType()
    {
        return type;
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
        SchemaColumn that = (SchemaColumn) o;
        return name.equals(that.name) && type.equals(that.type);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(name, type);
    }
}
