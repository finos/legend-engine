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
