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

package org.finos.legend.engine.persistence.components.relational.sqldom.schema;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;

public class VariableSizeDataType extends DataType
{
    private String type;
    private int length;
    private int scale;

    public VariableSizeDataType(String type, int length, int scale)
    {
        this.type = type;
        this.length = length;
        this.scale = scale;
    }

    public VariableSizeDataType(String type, int length)
    {
        this.type = type;
        this.length = length;
        this.scale = -1;
    }

    public VariableSizeDataType(String type)
    {
        this.type = type;
        this.length = -1;
        this.scale = -1;
    }

    public void setLength(int length)
    {
        this.length = length;
    }

    public void setScale(int scale)
    {
        this.scale = scale;
    }

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        if (length >= 0 && scale >= 0)
        {
            builder.append(String.format("%s(%d,%d)", type, length, scale));
        }
        else if (length >= 0)
        {
            builder.append(String.format("%s(%d)", type, length));
        }
        else
        {
            builder.append(type);
        }
    }

    @Override
    public void push(Object node)
    {

    }
}
