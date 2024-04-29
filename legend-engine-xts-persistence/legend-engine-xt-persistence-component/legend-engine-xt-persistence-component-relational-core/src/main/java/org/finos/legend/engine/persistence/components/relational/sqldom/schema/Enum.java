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

public class Enum extends DataType
{
    private final String[] values;

    public Enum(String[] values)
    {
        this.values = values;
    }

    @Override
    public void genSql(StringBuilder builder)
    {
        builder.append(String.format("ENUM(%s)", String.join(",", values)));
    }
}
