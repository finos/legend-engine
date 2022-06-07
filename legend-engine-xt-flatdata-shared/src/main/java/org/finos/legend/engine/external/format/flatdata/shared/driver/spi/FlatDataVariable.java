//  Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.external.format.flatdata.shared.driver.spi;

import java.util.Objects;

public final class FlatDataVariable
{
    private final String name;
    private final VariableType type;

    public FlatDataVariable(String name, VariableType type)
    {
        this.name = Objects.requireNonNull(name);
        this.type = Objects.requireNonNull(type);
    }

    public String getName()
    {
        return name;
    }

    public VariableType getType()
    {
        return type;
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof FlatDataVariable && name.equals(((FlatDataVariable) o).name);
    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    @Override
    public String toString()
    {
        return "FlatDataVariable{" +
                "name='" + name + '\'' +
                ", type=" + type +
                '}';
    }
}
