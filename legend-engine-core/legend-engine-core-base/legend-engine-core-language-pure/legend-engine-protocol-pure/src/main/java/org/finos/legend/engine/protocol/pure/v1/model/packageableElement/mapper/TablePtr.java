// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;

public class TablePtr
{
    public String _type;
    public String table;
    public String schema;
    public String database;
    public SourceInformation sourceInformation;

    public TablePtr()
    {
    }

    @JsonIgnore
    @BsonIgnore
    public String getDb()
    {
        return this.database;
    }
}
