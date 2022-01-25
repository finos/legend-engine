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

package org.finos.legend.engine.language.pure.dsl.service.execution;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity;
import org.finos.legend.engine.shared.core.operational.Assert;

public class ServiceVariable
{
    private final String name;
    private final Class<?> type;
    private final Multiplicity multiplicity;

    public ServiceVariable(String name, Class<?> type, Multiplicity multiplicity)
    {
        Assert.assertFalse(type.isPrimitive(), () -> "type should not be the primitive class");
        this.name = name;
        this.type = type;
        this.multiplicity = multiplicity;
    }

    public String getName()
    {
        return this.name;
    }

    public Class<?> getType()
    {
        return this.type;
    }

    public boolean isOptional()
    {
        return this.multiplicity.lowerBound < 1;
    }

    public boolean isToMany()
    {
        return this.multiplicity.isUpperBoundGreaterThan(1);
    }
}
