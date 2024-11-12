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
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.Conversion;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.ConversionContext;
import org.json.simple.JSONArray;

public class JsonDeserializationMultiplicityRange<T> extends JsonPropertyDeserialization<T>
{
    private final Long lowerBound;
    private final Long upperBound;
    private final String humanReadableMultiplicity;

    public JsonDeserializationMultiplicityRange(AbstractProperty property, boolean isFromAssociation, Conversion<Object, T> conversion, Type type, Multiplicity multiplicity)
    {
        super(property, isFromAssociation, conversion, type);
        this.upperBound = multiplicity._upperBound()._value();
        this.lowerBound = multiplicity._lowerBound()._value();
        this.humanReadableMultiplicity = org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(multiplicity);
    }

    @Override
    public RichIterable<T> apply(Object jsonValue, ConversionContext context)
    {
        JsonDeserializationContext jsonDeserializationContext = (JsonDeserializationContext) context;
        if (jsonValue == null)
        {
            if (!isFromAssociation() && (this.lowerBound > 0))
            {
                throw new PureExecutionException(jsonDeserializationContext.getSourceInformation(), "Expected value(s) of multiplicity " + this.humanReadableMultiplicity + ", found 0 value(s).", Stacks.mutable.empty());
            }
            return Lists.immutable.empty();
        }
        if (jsonValue instanceof JSONArray)
        {
            JSONArray jsonValues = (JSONArray) jsonValue;
            if ((jsonValues.size() < this.lowerBound) || (jsonValues.size() > this.upperBound))
            {
                throw new PureExecutionException(jsonDeserializationContext.getSourceInformation(), "Expected value(s) of multiplicity " + this.humanReadableMultiplicity + ", found " + jsonValues.size() + " value(s).", Stacks.mutable.empty());
            }
            return applyConversion((JSONArray) jsonValue, jsonDeserializationContext);
        }
        else
        {
            if ((this.lowerBound > 1) || (this.upperBound < 1))
            {
                throw new PureExecutionException(jsonDeserializationContext.getSourceInformation(), "Expected value(s) of multiplicity " + this.humanReadableMultiplicity + ", found 1 value(s).", Stacks.mutable.empty());
            }
            return this.applyConversion(jsonValue, jsonDeserializationContext);
        }
    }
}
