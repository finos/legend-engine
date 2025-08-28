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

package org.finos.legend.engine.query.sql.providers.core;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.collections.impl.utility.ListIterate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class TableSource
{
    private final String type;
    private final List<TableSourceArgument> arguments;

    public TableSource(String type, List<TableSourceArgument> arguments)
    {
        this.type = type;
        this.arguments = arguments == null ? Collections.emptyList() : arguments;
    }

    public String getType()
    {
        return type;
    }

    public List<TableSourceArgument> getArguments()
    {
        return this.arguments;
    }

    //get named argument, or default to index
    public TableSourceArgument getArgument(String name, int index)
    {
        return getArgument(name, index, true);
    }

    public TableSourceArgument getArgument(String name, int index, boolean required)
    {
        Optional<TableSourceArgument> found = getNamedArgument(name);
        return found.orElseGet(() ->
        {
            if (this.arguments.size() > index && index >= 0)
            {
                return this.arguments.get(index);
            }
            if (required)
            {
                throw new IllegalArgumentException("'" + name + "' parameter is required");
            }
            else
            {
                return null;
            }
        });
    }

    public Optional<TableSourceArgument> getNamedArgument(String name)
    {
        return ListIterate.select(this.arguments, a -> name.equals(a.getName())).getFirstOptional();
    }

    public <T> T getArgumentValueAs(String name, int index, Class<T> type, boolean required)
    {
        TableSourceArgument argument = getArgument(name, index, required);

        if (!required && argument == null)
        {
            return null;
        }
        Object value = argument.getValue();

        if (type.isInstance(value))
        {
            return (T) value;
        }

        throw new IllegalArgumentException("Argument of name '" + name + "' or index '" + index + "' is not of type " + type.getName());
    }

    @Override
    public boolean equals(Object o)
    {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this);
    }
}
