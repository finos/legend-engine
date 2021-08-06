package org.finos.legend.engine.external.shared.format.model.test;

import java.util.Objects;

public class ModelText
{
    private final String path;
    private final String grammar;

    public ModelText(String path, String grammar)
    {
        this.path = Objects.requireNonNull(path);
        this.grammar = Objects.requireNonNull(grammar);
    }

    public String getPath()
    {
        return path;
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof ModelText && o.toString().equals(toString());
    }

    @Override
    public int hashCode()
    {
        return path.hashCode();
    }

    @Override
    public String toString()
    {
        return ">>>" + path + "\n" + grammar;
    }
}
