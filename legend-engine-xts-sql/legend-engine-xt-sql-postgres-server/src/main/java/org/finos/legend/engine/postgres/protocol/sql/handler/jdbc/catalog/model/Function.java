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

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.postgres.protocol.wire.serialization.types.PGType;

public class Function
{
    private final  String name;

    private final  PGType<?> returnType;

    private final  MutableList<PGType<?>> parameterTypes;

    private Schema schema;

    private boolean returnSet;


    public Function(String name, MutableList<PGType<?>> parameters, PGType<?> returnType, boolean returnSet)
    {
        this.name = name;
        this.parameterTypes = parameters;
        this.returnType = returnType;
        this.returnSet = returnSet;
    }

    public String getName()
    {
        return this.name;
    }

    public void setSchema(Schema schema)
    {
        this.schema = schema;
    }

    public Schema getSchema()
    {
        return schema;
    }

    public MutableList<PGType<?>> getParameters()
    {
        return this.parameterTypes;
    }

    public PGType<?> getReturnType()
    {
        return returnType;
    }

    public boolean getReturnSet()
    {
        return returnSet;
    }
}
