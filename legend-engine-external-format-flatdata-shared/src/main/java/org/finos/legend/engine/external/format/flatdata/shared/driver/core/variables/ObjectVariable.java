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

package org.finos.legend.engine.external.format.flatdata.shared.driver.core.variables;

import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataVariable;

public class ObjectVariable<T>
{
    private final FlatDataProcessingContext context;
    private final FlatDataVariable variable;

    private ObjectVariable(FlatDataProcessingContext context, FlatDataVariable variable)
    {
        this.context = context;
        this.variable = variable;
    }

    public T set(T value)
    {
        return context.setVariableValue(variable.getName(), value);
    }

    public T get()
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

    public static <T> ObjectVariable<T> reference(FlatDataProcessingContext context, FlatDataVariable variable)
    {
        return new ObjectVariable<>(context, variable);
    }

    public static <T> ObjectVariable<T> initialize(FlatDataProcessingContext context, FlatDataVariable variable, T value)
    {
        ObjectVariable<T> result = new ObjectVariable<>(context, variable);
        result.set(value);
        return result;
    }

    public static <T> ObjectVariable<T> initializeIfMissing(FlatDataProcessingContext context, FlatDataVariable variable, T value)
    {
        return ObjectVariable.initialize(context, variable, context.getVariableValue(variable.getName(), value));
    }
}
