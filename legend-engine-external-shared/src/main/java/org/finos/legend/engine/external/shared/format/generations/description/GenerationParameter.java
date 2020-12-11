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

package org.finos.legend.engine.external.shared.format.generations.description;

import org.finos.legend.pure.generated.Root_meta_pure_generation_metamodel_GenerationParameter;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;

import java.util.List;
import java.util.stream.Collectors;

public class GenerationParameter implements GenerationProperty
{

    private static final String ROOT = "Root::";
    final Root_meta_pure_generation_metamodel_GenerationParameter parameter;

    public GenerationParameter(Root_meta_pure_generation_metamodel_GenerationParameter p)
    {
        this.parameter = p;
    }

    @Override
    public String getName()
    {
        return this.parameter._name();
    }

    @Override
    public String getDescription()
    {
        return this.parameter._description();
    }

    @Override
    public GenerationItemType getType()
    {
        return GenerationItemType.valueOf(this.parameter._type().toUpperCase());
    }

    @Override
    public GenerationPropertyItem getItems()
    {
        return (this.parameter._items() == null) ? null : new GenerationPropertyItem()
        {
            @Override
            public List<GenerationItemType> getTypes()
            {
                return parameter._items()._types().toList().stream().map(t -> GenerationItemType.valueOf(String.valueOf(t).toUpperCase())).collect(Collectors.toList());
            }

            @Override
            public List<String> getEnums()
            {
                return parameter._items()._enums().toList().stream()
                        .map(e -> (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum) e)
                        .map(theEnum -> theEnum.getFullSystemPath().replace(ROOT, "") + "." + theEnum.getName()).collect(Collectors.toList());
            }
        };
    }

    @Override
    public String getDefaultValue()
    {
        if (this.parameter._defaultValue() instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum)
        {
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum theEnum = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum) this.parameter._defaultValue();
            return theEnum.getFullSystemPath().replace(ROOT, "") + "." + theEnum.getName();
        }
        if (this.parameter._defaultValue() instanceof PureMap)
        {
            PureMap theMap = (PureMap) this.parameter._defaultValue();
            return String.valueOf(theMap.getMap());
        }
        return String.valueOf(this.parameter._defaultValue());
    }

    @Override
    public boolean getRequired()
    {
        return this.parameter._required();
    }
}
