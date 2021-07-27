package org.finos.legend.engine.external.format.flatdata.shared.driver.core.variables;

import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataVariable;

public class IntegerVariable
{
    private final FlatDataProcessingContext<?> context;
    private final FlatDataVariable variable;

    private IntegerVariable(FlatDataProcessingContext<?> context, FlatDataVariable variable)
    {
        this.context = context;
        this.variable = variable;
    }

    public long set(long value)
    {
        return context.setVariableValue(variable.getName(), value);
    }

    public long increment()
    {
        return this.set(this.get() + 1);
    }

    public long decrement()
    {
        return this.set(this.get() - 1);
    }

    public long get()
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

    public static IntegerVariable reference(FlatDataProcessingContext context, FlatDataVariable variable)
    {
        return new IntegerVariable(context, variable);
    }

    public static IntegerVariable initialize(FlatDataProcessingContext context, FlatDataVariable variable, long value)
    {
        IntegerVariable result = new IntegerVariable(context, variable);
        result.set(value);
        return result;
    }

    public static IntegerVariable initializeIfMissing(FlatDataProcessingContext<?> context, FlatDataVariable variable, long value)
    {
        return IntegerVariable.initialize(context, variable, context.getVariableValue(variable.getName(), value));
    }
}
