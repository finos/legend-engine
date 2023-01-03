package org.finos.legend.engine.testable.persistence.model;

public class ActiveRowsFilterCondition
{
    private String column;
    private Object value;

    public ActiveRowsFilterCondition(String column, Object value)
    {
        this.column = column;
        this.value = value;
    }

    public String getColumn()
    {
        return column;
    }

    public Object getValue()
    {
        return value;
    }
}
