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

package org.finos.legend.engine.language.pure.compiler.toPureGraph.defect;

import java.util.Arrays;

/**
 * The severity of a type of defect, which the system property {@code legend.engine.pureModel.defect.<defectTypeId>}
 * overrides with the name of a {@link DefectSeverityLevel}.
 */
public final class DefectSeverity
{
    public static final String PROPERTY_PREFIX = "legend.engine.pureModel.defect.";

    private DefectSeverity()
    {
    }

    public static DefectSeverityLevel of(String defectTypeId, DefectSeverityLevel defaultSeverityLevel)
    {
        if (defectTypeId == null)
        {
            return defaultSeverityLevel;
        }
        String property = PROPERTY_PREFIX + defectTypeId;
        String value = System.getProperty(property);
        if (value == null)
        {
            return defaultSeverityLevel;
        }
        try
        {
            return DefectSeverityLevel.valueOf(value);
        }
        catch (IllegalArgumentException e)
        {
            throw new IllegalArgumentException("Invalid value '" + value + "' for system property '" + property + "', expected one of " + Arrays.toString(DefectSeverityLevel.values()), e);
        }
    }
}
