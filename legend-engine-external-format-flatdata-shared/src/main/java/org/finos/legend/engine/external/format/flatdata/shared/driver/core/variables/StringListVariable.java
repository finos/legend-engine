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

import java.util.Collections;
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

    public List<String> set(String value)
    {
        return set(Collections.singletonList(value));
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
