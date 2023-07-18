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

package org.finos.legend.engine.language.pure.dsl.generation.config;

import org.apache.commons.lang3.StringUtils;
import org.finos.legend.engine.external.shared.format.generations.GenerationConfiguration;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.fileGeneration.ConfigurationProperty;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.fileGeneration.FileGenerationSpecification;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigBuilder
{
    public static void setScopeElements(FileGenerationSpecification fileGeneration, GenerationConfiguration generationConfiguration)
    {
        generationConfiguration.scopeElements = fileGeneration.scopeElements;
    }

    public static void setConfigurationProperty(FileGenerationSpecification fileGeneration, ConfigurationProperty configurationProperty, GenerationConfiguration generationConfiguration)
    {
        Class<?> clazz = generationConfiguration.getClass();
        String paramName = configurationProperty.name;
        Object value = configurationProperty.value;
        Field field;
        try
        {
            Object fieldValue = value;
            field = clazz.getField(paramName);
            if (field.getType().isEnum())
            {
                fieldValue = Enum.valueOf((Class<Enum>) field.getType(), (String) fieldValue);
            }
            Assert.assertTrue(field.getType().isAssignableFrom(fieldValue.getClass()), () -> "Type '" + value.getClass().getSimpleName() + "' not assignable to type '" + field.getType().getSimpleName() + "' for config property '" + paramName + "'");
            field.setAccessible(true);
            field.set(generationConfiguration, fieldValue);
        }
        catch (Exception e)
        {
            String setMethodName = "set" + StringUtils.capitalize(paramName);
            try
            {
                Method m = clazz.getMethod(setMethodName, value.getClass());
                m.invoke(generationConfiguration, value);
            }
            catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException error)
            {
                throw new EngineException("Configuration property '" + paramName + "' not found in '" + fileGeneration.type + "' config");
            }
        }
    }

    public static void duplicateCheck(List<ConfigurationProperty> properties)
    {
        Set<String> configPropertyNames = new HashSet<>();
        properties.forEach(configurationProperty ->
        {
            if (!configPropertyNames.add(configurationProperty.name))
            {
                throw new EngineException("Duplicated configuration property name: '" + configurationProperty.name + "'");
            }
        });
    }

    public static void noConfigurationPropertiesCheck(FileGenerationSpecification fileGeneration)
    {
        if (!fileGeneration.configurationProperties.isEmpty())
        {
            throw new EngineException("File generation type '" + fileGeneration.type + "' has no configurable properties");
        }
    }
}
