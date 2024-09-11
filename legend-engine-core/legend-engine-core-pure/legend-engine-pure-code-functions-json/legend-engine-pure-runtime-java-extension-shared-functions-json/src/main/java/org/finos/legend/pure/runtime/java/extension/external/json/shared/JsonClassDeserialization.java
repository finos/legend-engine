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
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.ClassConversion;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.Conversion;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.ConversionContext;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.ObjectFactory;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.PrimitiveConversion;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.PropertyConversion;
import org.json.simple.JSONObject;

import java.util.Set;

public class JsonClassDeserialization<T extends Any> extends ClassConversion<Object, T>
{
    private final MutableSet<String> knownJsonProperties = Sets.mutable.empty();

    public JsonClassDeserialization(Class<T> clazz)
    {
        super(clazz);
    }

    @Override
    protected void completeInitialisation(ConversionContext context)
    {
        super.completeInitialisation(context);
        this.knownJsonProperties.add(((JsonDeserializationContext) context).getTypeKeyName());
        getProperties(context.getProcessorSupport()).collect(CoreInstance::getName, this.knownJsonProperties);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected PropertyConversion<?, ?> newMultiplicityOneConversion(AbstractProperty abstractProperty, Conversion<?, ?> conversion, Type type)
    {
        return new JsonDeserializationMultiplicityOne(abstractProperty, abstractProperty._owner() instanceof Association, conversion, type);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected PropertyConversion<?, ?> newMultiplicityManyConversion(AbstractProperty abstractProperty, Conversion<?, ?> conversion, Type type)
    {
        return new JsonDeserializationMultiplicityMany(abstractProperty, abstractProperty._owner() instanceof Association, conversion, type);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected PropertyConversion<?, ?> newMultiplicityParameterisedConversion(AbstractProperty abstractProperty, Conversion<?, ?> conversion, Type type)
    {
        return new JsonDeserializationMultiplicityParameterised(abstractProperty, abstractProperty._owner() instanceof Association, conversion, type);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected PropertyConversion<?, ?> newMultiplicityOptionalConversion(AbstractProperty abstractProperty, Conversion<?, ?> conversion, Type type)
    {
        return new JsonDeserializationMultiplicityOptional(abstractProperty, abstractProperty._owner() instanceof Association, conversion, type);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected PropertyConversion<?, ?> newMultiplicityRangeConversion(AbstractProperty abstractProperty, Conversion<?, ?> conversion, Type type)
    {
        return new JsonDeserializationMultiplicityRange(abstractProperty, abstractProperty._owner() instanceof Association, conversion, type, abstractProperty._multiplicity());
    }

    @SuppressWarnings("unchecked")
    @Override
    public T apply(Object input, ConversionContext context)
    {
        if (!(input instanceof JSONObject))
        {
            // value is a not a JSON object but number or string, a reference to another object.
            // Currently, this should be ignored.
            return null;
        }

        JsonDeserializationContext deserializationContext = (JsonDeserializationContext) context;
        ObjectFactory objectFactory = deserializationContext.getObjectFactory();

        JSONObject jsonObject = (JSONObject) input;
        MutableMap<String, RichIterable<?>> propertyKeyValues = keyValueProperties(jsonObject, deserializationContext);
        try
        {
            return (T) objectFactory.newObject(this.clazz, propertyKeyValues);
        }
        catch (PureExecutionException e)
        {
            throw new PureExecutionException(deserializationContext.getSourceInformation(), "Could not create new instance of " + this.pureTypeAsString() + ": \n" + e.getInfo(), e);
        }
    }

    /**
     * Generates a mapping of property name to a collection of values which describe the properties of the newly instantiated object.
     */
    private MutableMap<String, RichIterable<?>> keyValueProperties(JSONObject jsonObject, JsonDeserializationContext context)
    {
        failOnUnknownProperties(jsonObject, context);
        MutableMap<String, RichIterable<?>> keyValues = Maps.mutable.empty();
        for (Conversion<?, ?> propertyConversion : this.propertyConversions)
        {
            JsonPropertyDeserialization<?> jsonPropertyDeserialization = (JsonPropertyDeserialization<?>) propertyConversion;
            Object jsonValue = jsonObject.get(jsonPropertyDeserialization.getName());
            try
            {
                RichIterable<?> values = jsonPropertyDeserialization.apply(jsonValue, context);
                if (!values.isEmpty()) // can't create new objects from properties that have an empty collection of values, instead the property should just be ignored
                {
                    keyValues.put(jsonPropertyDeserialization.getName(), values);
                }
            }
            catch (PureExecutionException e)
            {
                throw new PureExecutionException(context.getSourceInformation(), "Error populating property '" + jsonPropertyDeserialization.getName() + "' on class '" + this.pureTypeAsString() + "': \n" + e.getInfo(), e);
            }
            catch (ClassCastException | IllegalArgumentException e)
            {
                String foundType = jsonValue instanceof JSONObject ? "JSON Object" : PrimitiveConversion.toPurePrimitiveName(jsonValue.getClass());
                throw new PureExecutionException(context.getSourceInformation(), "Error populating property '" + jsonPropertyDeserialization.getName() + "' on class '" + this.pureTypeAsString() + "': \nExpected " + jsonPropertyDeserialization.pureTypeAsString() + ", found " + foundType, e);
            }
        }
        return keyValues;
    }

    /**
     * If the failOnUnknownProperties flag is set in the JSONDeserializationConfig (and as a result in the JsonDeserializationContext),
     * then ensure that no additional data is provided in the JSONObjects modelling instances of PURE classes.
     * This is relatively expensive to compute but the flag is rarely set.
     */
    @SuppressWarnings("unchecked")
    private void failOnUnknownProperties(JSONObject jsonValue, JsonDeserializationContext context)
    {
        if (context.isFailOnUnknownProperties())
        {
            MutableList<String> unknownProperties = Iterate.reject((Set<String>) jsonValue.keySet(), this.knownJsonProperties::contains, Lists.mutable.empty());
            if (unknownProperties.notEmpty())
            {
                StringBuilder errorMsg = new StringBuilder();
                unknownProperties.forEach(p -> PackageableElement.writeUserPathForPackageableElement(errorMsg.append("Property '").append(p).append("' can't be found in class "), this.clazz).append(". "));
                throw new PureExecutionException(context.getSourceInformation(), errorMsg.toString());
            }
        }
    }
}
