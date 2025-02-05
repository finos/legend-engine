// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.protocol.pure.m3.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.m3.function.Lambda;

public class Unit
{
    public String name;
    public String measure;
    public Lambda conversionFunction;
    public SourceInformation sourceInformation;

    @Deprecated
    public Unit()
    {
    }

    private Unit(String name, String measure, Lambda conversionFunction, SourceInformation sourceInformation)
    {
        this.name = name;
        this.measure = measure;
        this.conversionFunction = conversionFunction;
        this.sourceInformation = sourceInformation;
    }

    public static Unit newUnit(String name, String measure, Lambda conversionFunction, SourceInformation sourceInformation)
    {
        return new Unit(name, measure, conversionFunction, sourceInformation);
    }

    public static Unit newUnit(String name, String measure, SourceInformation sourceInformation)
    {
        return newUnit(name, measure, null, sourceInformation);
    }

    @JsonCreator
    public static Unit newUnit(
            @JsonProperty("name") String name,
            @JsonProperty("measure") String measure,
            @JsonProperty("conversionFunction") Lambda conversionFunction,
            @JsonProperty("sourceInformation") SourceInformation sourceInformation,
            // for backward compatibility
            @Deprecated @JsonProperty("_type") String type,
            @Deprecated @JsonProperty("package") String _package,
            @Deprecated @JsonProperty("superType") String superType
    )
    {
        return newUnit(name, measure, conversionFunction, sourceInformation);
    }
}
