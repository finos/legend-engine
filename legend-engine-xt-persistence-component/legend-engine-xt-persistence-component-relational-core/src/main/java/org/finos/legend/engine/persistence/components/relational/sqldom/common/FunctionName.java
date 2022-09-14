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

package org.finos.legend.engine.persistence.components.relational.sqldom.common;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum FunctionName
{
    SUM("SUM"),
    MAX("MAX"),
    MIN("MIN"),
    COUNT("COUNT"),
    COALESCE("COALESCE"),
    CURRENT_TIME("CURRENT_TIME"),
    CURRENT_DATE("CURRENT_DATE"),
    HASH("HASH"),
    CONCAT("CONCAT"),
    RAW_TO_HEX("RAWTOHEX"),
    CURRENT_TIMESTAMP("CURRENT_TIMESTAMP"),
    SYSDATE("SYSDATE");

    private static final Map<String, FunctionName> BY_NAME = Arrays
        .stream(FunctionName.values())
        .collect(Collectors.toMap(FunctionName::get, java.util.function.Function.identity()));

    public static FunctionName fromName(String name)
    {
        return BY_NAME.get(name);
    }

    private final String name;

    FunctionName(String name)
    {
        this.name = name;
    }

    public String get()
    {
        return name;
    }
}
