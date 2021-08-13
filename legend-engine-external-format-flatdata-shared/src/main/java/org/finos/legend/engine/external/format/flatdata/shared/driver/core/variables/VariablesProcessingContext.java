// Copyright 2021 Goldman Sachs
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class VariablesProcessingContext<T> implements FlatDataProcessingContext<T>
{
    private final ProcessingVariables variables;
    private final Set<String> declaredVariableNames = new HashSet<>();

    public VariablesProcessingContext(ProcessingVariables variables, List<FlatDataVariable> declared)
    {
        this.variables = variables;
        declared.forEach(v -> this.declaredVariableNames.add(v.getName()));
    }

    @Override
    public boolean hasVariableValue(String variableName)
    {
        checkDeclared(variableName);
        return variables.hasVariableValue(variableName);
    }

    @Override
    public <X> X getVariableValue(String variableName)
    {
        checkDeclared(variableName);
        return variables.getVariableValue(variableName);
    }

    @Override
    public <X> X getVariableValue(String variableName, X defaultValue)
    {
        checkDeclared(variableName);
        return variables.getVariableValue(variableName, defaultValue);
    }

    @Override
    public <X> X setVariableValue(String variableName, X value)
    {
        checkDeclared(variableName);
        return variables.setVariableValue(variableName, value);
    }

    private void checkDeclared(String variableName)
    {
        if (!declaredVariableNames.contains(variableName))
        {
            throw new IllegalArgumentException("Cannot use an undeclared variable: " + variableName);
        }
    }
}
