// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.protocol.pure.v1.model.type;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;

import java.util.List;

public class GenericType
{
    public Type rawType;
    public List<GenericType> typeArguments = Lists.mutable.empty();
    public List<Multiplicity> multiplicityArguments = Lists.mutable.empty();
    public List<ValueSpecification> typeVariables = Lists.mutable.empty();
    public SourceInformation sourceInformation;

    public GenericType()
    {
    }

    public GenericType(Type rawType)
    {
        this.rawType = rawType;
    }

    public GenericType(Type rawType, List<GenericType> typeArguments)
    {
        this.rawType = rawType;
        this.typeArguments = typeArguments;
    }

    public GenericType(Type rawType, List<GenericType> typeArguments, List<Multiplicity> multiplicityArguments)
    {
        this.rawType = rawType;
        this.typeArguments = typeArguments;
        this.multiplicityArguments = multiplicityArguments;
    }
}
