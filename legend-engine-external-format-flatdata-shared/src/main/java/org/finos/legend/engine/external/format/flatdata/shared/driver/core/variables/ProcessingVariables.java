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

import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataDriverDescription;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataVariable;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.VariableType;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatData;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcessingVariables
{
    private final List<FlatDataDriverDescription> descriptions = FlatDataDriverDescription.loadAll();

    private final Map<String, VariableType> variableTypes = new HashMap<>();
    private final Map<String, Object> variableValues = new HashMap<>();

    public ProcessingVariables(FlatData schema)
    {
        for (FlatDataSection section : schema.getSections())
        {
            FlatDataDriverDescription description = descriptions.stream().filter(d -> d.getId().equals(section.getDriverId())).findFirst().orElseThrow(() -> new RuntimeException("No driver for: '" + section.getDriverId() + "'"));
            description.getDeclares().forEach(this::declare);
        }
    }

    public boolean hasVariableValue(String variableName)
    {
        return variableValues.containsKey(variableName);
    }

    public <T> T getVariableValue(String variableName)
    {
        if (!variableValues.containsKey(variableName))
        {
            throw new IllegalArgumentException("Variable '" + variableName + "' has not been assigned");
        }
        return (T) variableValues.get(variableName);
    }

    public <T> T getVariableValue(String variableName, T defaultValue)
    {
        return (T) variableValues.getOrDefault(variableName, defaultValue);
    }

    public <T> T setVariableValue(String variableName, T value)
    {
        if (!variableTypes.containsKey(variableName))
        {
            throw new IllegalArgumentException("Variable '" + variableName + "' has not been declared");
        }
        VariableType type = variableTypes.get(variableName);
        if (type == VariableType.Integer && value instanceof Long || value instanceof Integer)
        {
            variableValues.put(variableName, value);
        }
        else if (type == VariableType.String && value instanceof String)
        {
            variableValues.put(variableName, value);
        }
        else
        {
            throw new IllegalArgumentException("Variable '" + variableName + "' is declared as " + type + " and cannot be assigned a " + value.getClass().getSimpleName());
        }
        return value;
    }

    private void declare(FlatDataVariable variable)
    {
        String name = variable.getName();
        if (variableTypes.containsKey(name) && variableTypes.get(name) != variable.getType())
        {
            throw new IllegalArgumentException("Variable '" + name + "' already declard as " + variableTypes.get(name) + " cannot be re-declared as " + variable.getType());
        }
        variableTypes.put(name, variable.getType());
    }
}
