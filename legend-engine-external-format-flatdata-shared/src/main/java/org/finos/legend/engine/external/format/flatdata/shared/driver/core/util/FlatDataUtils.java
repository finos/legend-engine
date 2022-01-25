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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FlatDataUtils
{
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
