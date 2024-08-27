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

package org.finos.legend.pure.runtime.java.extension.external.json.shared;

import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.Conversion;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.ConversionContext;

public class JsonQualifiedPropertySerialization<T> implements Conversion<T, Object>
{
    private final QualifiedProperty qualifiedProperty;
    private final String name;
    private final Type type;
    private final Multiplicity multiplicity;
    private final Conversion conversion;

    public JsonQualifiedPropertySerialization(QualifiedProperty qualifiedProperty, String name, Type type, Multiplicity multiplicity, Conversion conversion)
    {
        this.qualifiedProperty = qualifiedProperty;
        this.name = name;
        this.type = type;
        this.multiplicity = multiplicity;
        this.conversion = conversion;
    }

    @Override
    public Object apply(T pureObject, ConversionContext context)
    {
        JsonSerializationContext jsonSerializationContext = (JsonSerializationContext)context;
        Object evaluatedProperty = jsonSerializationContext.evaluateQualifiedProperty(pureObject, this.qualifiedProperty, this.type, this.multiplicity, this.name);
        return this.conversion.apply(evaluatedProperty, jsonSerializationContext);
    }

    @Override
    public String pureTypeAsString()
    {
        return this.type.getName();
    }

    public String getName()
    {
        return this.name;
    }
}
