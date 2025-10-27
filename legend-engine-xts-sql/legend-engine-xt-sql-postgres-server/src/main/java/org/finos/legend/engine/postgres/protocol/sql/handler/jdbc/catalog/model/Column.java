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

import org.finos.legend.engine.postgres.protocol.wire.serialization.types.PGType;

public class Column
{
    static int _columnId = 20000;

    private final  String name;

    private final  PGType<?> type;

    private final  int columnId;

    public Column(String name, PGType<?> type)
    {
        this.name = name;
        this.type = type;
        this.columnId = _columnId++;
    }

    public String getName()
    {
        return this.name;
    }

    public PGType<?> getType()
    {
        return this.type;
    }

    public int getColumnId()
    {
        return this.columnId;
    }
}
