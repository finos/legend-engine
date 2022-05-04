package org.finos.legend.engine.external.format.flatdata.shared.driver.core.variables;

import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataVariable;

public class StringVariable
{
    private final FlatDataProcessingContext context;
    private final FlatDataVariable variable;

    private StringVariable(FlatDataProcessingContext context, FlatDataVariable variable)
    {
        this.context = context;
        this.variable = variable;
    }

    public String set(String value)
    {
        return context.setVariableValue(variable.getName(), value);
    }

    public String get()
    {
        return context.getVariableValue(variable.getName());
    }

    public boolean isSet()
    {
        return context.hasVariableValue(variable.getName());
    }

    @Override
    public String toString()
    {
        return variable.getName() + "=" + get();
    }

    public static StringVariable reference(FlatDataProcessingContext context, FlatDataVariable variable)
    {
        return new StringVariable(context, variable);
    }

    public static StringVariable initialize(FlatDataProcessingContext context, FlatDataVariable variable, String value)
    {
        StringVariable result = new StringVariable(context, variable);
        result.set(value);
        return result;
    }

    public static StringVariable initializeIfMissing(FlatDataProcessingContext context, FlatDataVariable variable, String value)
    {
        return StringVariable.initialize(context, variable, context.getVariableValue(variable.getName(), value));
    }
}
