// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.common;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum FilterType
{
    GREATER_THAN("GT"),
    GREATER_THAN_EQUAL("GTE"),
    LESS_THAN("LT"),
    LESS_THAN_EQUAL("LTE"),
    EQUAL_TO("EQ");

    private String type;

    FilterType(String type)
    {
        this.type = type;
    }

    public String getType()
    {
        return type;
    }

    private static final Map<String, FilterType> BY_NAME = Arrays
            .stream(FilterType.values())
            .collect(Collectors.toMap(FilterType::getType, java.util.function.Function.identity()));

    public static FilterType fromName(String name)
    {
        return BY_NAME.get(name);
    }
}
