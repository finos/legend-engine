package org.finos.legend.engine.external.format.flatdata.shared.driver.core.variables;

import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataVariable;

import java.util.List;

public class StringListVariable
{
    private final FlatDataProcessingContext context;
    private final FlatDataVariable variable;

    private StringListVariable(FlatDataProcessingContext context, FlatDataVariable variable)
    {
        this.context = context;
        this.variable = variable;
    }

    public List<String> set(List<String> values)
    {
        return context.setVariableValue(variable.getName(), values);
    }

    public List<String> get()
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

    public static StringListVariable reference(FlatDataProcessingContext context, FlatDataVariable variable)
    {
        return new StringListVariable(context, variable);
    }

    public static StringListVariable initialize(FlatDataProcessingContext context, FlatDataVariable variable, List<String> values)
    {
        StringListVariable result = new StringListVariable(context, variable);
        result.set(values);
        return result;
    }

    public static StringListVariable initializeIfMissing(FlatDataProcessingContext context, FlatDataVariable variable, List<String> values)
    {
        return StringListVariable.initialize(context, variable, context.getVariableValue(variable.getName(), values));
    }
}
