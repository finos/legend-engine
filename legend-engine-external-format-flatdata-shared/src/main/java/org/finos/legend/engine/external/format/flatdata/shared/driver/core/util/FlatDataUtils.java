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

package org.finos.legend.engine.external.format.flatdata.shared.driver.core.util;

import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataProperty;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataVariable;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.PropertyDescription;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.VariableType;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FlatDataUtils
{
    public static final String TIME_ZONE = "UTC";
    public static final String ISO_DATE_FORMAT = "yyyy-MM-dd";
    public static final String ISO_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXX";

    public static final String DEFAULT_DATE_FORMAT = "defaultDateFormat";
    public static final String DEFAULT_DATETIME_FORMAT = "defaultDateTimeFormat";
    public static final String DEFAULT_TIME_ZONE = "defaultTimeZone";
    public static final String DEFAULT_TRUE_STRING = "defaultTrueString";
    public static final String DEFAULT_FALSE_STRING = "defaultFalseString";
    public static final String DEFAULT_INTEGER_FORMAT = "defaultIntegerFormat";
    public static final String DEFAULT_DECIMAL_FORMAT = "defaultDecimalFormat";

    public static final FlatDataVariable VARIABLE_DEFAULT_DATE_FORMAT = new FlatDataVariable(DEFAULT_DATE_FORMAT, VariableType.StringList);
    public static final FlatDataVariable VARIABLE_DEFAULT_DATETIME_FORMAT = new FlatDataVariable(DEFAULT_DATETIME_FORMAT, VariableType.StringList);
    public static final FlatDataVariable VARIABLE_DEFAULT_TIME_ZONE = new FlatDataVariable(DEFAULT_TIME_ZONE, VariableType.String);
    public static final FlatDataVariable VARIABLE_DEFAULT_TRUE_STRING = new FlatDataVariable(DEFAULT_TRUE_STRING, VariableType.String);
    public static final FlatDataVariable VARIABLE_DEFAULT_FALSE_STRING = new FlatDataVariable(DEFAULT_FALSE_STRING, VariableType.String);
    public static final FlatDataVariable VARIABLE_DEFAULT_INTEGER_FORMAT = new FlatDataVariable(DEFAULT_INTEGER_FORMAT, VariableType.String);
    public static final FlatDataVariable VARIABLE_DEFAULT_DECIMAL_FORMAT = new FlatDataVariable(DEFAULT_DECIMAL_FORMAT, VariableType.String);

    public static List<PropertyDescription> dataTypeParsingProperties()
    {
        return new PropertyDescription.Builder()
                .optionalStringProperty(DEFAULT_DATE_FORMAT)
                .optionalStringProperty(DEFAULT_DATETIME_FORMAT)
                .optionalStringProperty(DEFAULT_TIME_ZONE)
                .optionalStringProperty(DEFAULT_TRUE_STRING)
                .optionalStringProperty(DEFAULT_FALSE_STRING)
                .optionalStringProperty(DEFAULT_INTEGER_FORMAT)
                .optionalStringProperty(DEFAULT_DECIMAL_FORMAT)
                .build();
    }

    public static List<FlatDataVariable> dataTypeParsingVariables()
    {
        return Arrays.asList(
                VARIABLE_DEFAULT_DATE_FORMAT,
                VARIABLE_DEFAULT_DATETIME_FORMAT,
                VARIABLE_DEFAULT_TIME_ZONE,
                VARIABLE_DEFAULT_TRUE_STRING,
                VARIABLE_DEFAULT_FALSE_STRING,
                VARIABLE_DEFAULT_INTEGER_FORMAT,
                VARIABLE_DEFAULT_DECIMAL_FORMAT
        );
    }

    public static Optional<String> getString(List<FlatDataProperty> properties, String... names)
    {
        String name = String.join(".", names);
        return properties.stream().filter(p -> p.getName().equals(name)).findFirst().map(p -> (String) p.getValues().get(0));
    }

    public static Optional<List<String>> getStrings(List<FlatDataProperty> properties, String... names)
    {
        String name = String.join(".", names);
        return properties.stream().filter(p -> p.getName().equals(name)).findFirst().map(p -> p.getValues().stream().map(String.class::cast).collect(Collectors.toList()));
    }

    public static Optional<Long> getInteger(List<FlatDataProperty> properties, String... names)
    {
        String name = String.join(".", names);
        return properties.stream().filter(p -> p.getName().equals(name)).findFirst().map(p -> (Long) p.getValues().get(0));
    }

    public static Optional<List<Long>> getIntegers(List<FlatDataProperty> properties, String... names)
    {
        String name = String.join(".", names);
        return properties.stream().filter(p -> p.getName().equals(name)).findFirst().map(p -> p.getValues().stream().map(Long.class::cast).collect(Collectors.toList()));
    }

    public static boolean getBoolean(List<FlatDataProperty> properties, String... names)
    {
        String name = String.join(".", names);
        return (boolean) properties.stream().filter(p -> p.getName().equals(name)).findFirst().map(p -> p.getValues().get(0)).orElse(false);
    }

    public static void setString(String text, List<FlatDataProperty> properties, String... names)
    {
        FlatDataProperty property = new FlatDataProperty(String.join(".", names), text);
        properties.add(property);
    }
}
