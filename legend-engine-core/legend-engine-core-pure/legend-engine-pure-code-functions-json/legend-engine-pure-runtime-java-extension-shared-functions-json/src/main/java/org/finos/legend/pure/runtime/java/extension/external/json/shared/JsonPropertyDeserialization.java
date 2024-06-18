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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.ClassConversion;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.Conversion;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.PropertyDeserialization;
import org.json.simple.JSONArray;

public abstract class JsonPropertyDeserialization<T> extends PropertyDeserialization<Object, T>
{
    public JsonPropertyDeserialization(AbstractProperty property, boolean isFromAssociation, Conversion<Object, T> conversion, Type type)
    {
        super(property, isFromAssociation, conversion, type);
    }

    @SuppressWarnings("unchecked")
    protected RichIterable<T> applyConversion(JSONArray jsonValue, JsonDeserializationContext context)
    {
        return Iterate.flatCollect(jsonValue, v -> applyConversion(v, context), Lists.mutable.empty());
    }

    protected RichIterable<T> applyConversion(Object jsonValue, JsonDeserializationContext context)
    {
        Conversion<Object, T> conversion = this.getConversion(jsonValue, context);
        T output = conversion.apply(jsonValue, context);
        return (output == null) ? Lists.immutable.empty() : Lists.immutable.with(output);
    }

    @SuppressWarnings("unchecked")
    private Conversion<Object, T> getConversion(Object jsonValue, JsonDeserializationContext context)
    {
        if (this.conversion instanceof ClassConversion)
        {
            Type resolvedType = JsonDeserializer.resolveType(this.type, jsonValue, context.getTypeKeyName(), context.getTypeLookup(), context.getSourceInformation());
            if (!resolvedType.equals(this.type))
            {
                return (Conversion<Object, T>) context.getConversionCache().getConversion(resolvedType, context);
            }
        }
        return this.conversion;
    }
}
