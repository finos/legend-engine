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

package org.finos.legend.engine.external.format.flatdata.shared.driver.core.variables;

import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataVariable;

public class IntegerVariable
{
    private final FlatDataProcessingContext context;
    private final FlatDataVariable variable;

    private IntegerVariable(FlatDataProcessingContext context, FlatDataVariable variable)
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

    public static IntegerVariable initializeIfMissing(FlatDataProcessingContext context, FlatDataVariable variable, long value)
    {
        return IntegerVariable.initialize(context, variable, context.getVariableValue(variable.getName(), value));
    }
}
