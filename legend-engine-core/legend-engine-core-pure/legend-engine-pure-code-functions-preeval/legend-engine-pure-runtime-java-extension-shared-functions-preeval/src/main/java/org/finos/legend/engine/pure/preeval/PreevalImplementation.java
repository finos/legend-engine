// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.pure.preeval;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public enum PreevalImplementation
{
    PURE,
    JAVA,
    SHADOW;

    public static final String SYSTEM_PROPERTY = "legend.engine.preeval.implementation";

    public static PreevalImplementation current()
    {
        return parse(System.getProperty(SYSTEM_PROPERTY));
    }

    public static PreevalImplementation parse(String value)
    {
        if (value == null || value.trim().isEmpty())
        {
            return PURE;
        }
        try
        {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException e)
        {
            String expected = Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Invalid value '" + value + "' for system property " + SYSTEM_PROPERTY + "; expected one of " + expected, e);
        }
    }
}
