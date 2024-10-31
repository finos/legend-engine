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
import org.eclipse.collections.api.factory.Stacks;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.Conversion;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.ConversionContext;
import org.json.simple.JSONArray;

public class JsonDeserializationMultiplicityOptional<T> extends JsonPropertyDeserialization<T>
{
    public JsonDeserializationMultiplicityOptional(AbstractProperty property, boolean isFromAssociation, Conversion<Object, T> conversion, Type type)
    {
        super(property, isFromAssociation, conversion, type);
    }

    @Override
    public RichIterable<T> apply(Object jsonValue, ConversionContext context)
    {
        JsonDeserializationContext jsonDeserializationContext = (JsonDeserializationContext) context;
        if (jsonValue == null)
        {
            return Lists.immutable.empty();
        }
        if (jsonValue instanceof JSONArray)
        {
            JSONArray jsonValues = (JSONArray) jsonValue;
            if (jsonValues.size() > 1)
            {
                throw new PureExecutionException(jsonDeserializationContext.getSourceInformation(), "Expected value(s) of multiplicity [0..1], found " + jsonValues.size() + " value(s).", Stacks.mutable.empty());
            }
            if (jsonValues.isEmpty())
            {
                return Lists.immutable.empty();
            }
            return applyConversion(jsonValues.get(0), jsonDeserializationContext);
        }
        return applyConversion(jsonValue, jsonDeserializationContext);
    }
}
