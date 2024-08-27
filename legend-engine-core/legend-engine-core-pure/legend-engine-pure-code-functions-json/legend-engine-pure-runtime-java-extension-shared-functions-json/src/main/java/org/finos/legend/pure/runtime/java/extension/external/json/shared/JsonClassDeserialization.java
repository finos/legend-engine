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
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.ClassConversion;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.Conversion;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.ConversionContext;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.ObjectFactory;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.PrimitiveConversion;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.PropertyConversion;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JsonClassDeserialization<T extends Any> extends ClassConversion<Object, T>
{
    private Predicate<String> isUnknownJsonProperty;

    private static final Function<Property, String> PROPERTY_NAME_FUNC = new Function<Property, String>()
    {
        @Override
        public String valueOf(Property property)
        {
            return property.getName();
        }
    };

    public JsonClassDeserialization(Class<T> clazz)
    {
        super(clazz);
        this.isUnknownJsonProperty = null;
    }

    @Override
    protected void completeInitialisation(ConversionContext context)
    {
        super.completeInitialisation(context);
        this.isUnknownJsonProperty = this.createUnknownJsonPropertyTest((JsonDeserializationContext)context);
    }

    private Predicate<String> createUnknownJsonPropertyTest(JsonDeserializationContext context)
    {
        final Set<String> knownJsonProperties = this.getProperties(context.getProcessorSupport()).collect(PROPERTY_NAME_FUNC).toSet();
        knownJsonProperties.add(context.getTypeKeyName());
        return new Predicate<String>()
        {
            @Override
            public boolean accept(String name)
            {
                return !knownJsonProperties.contains(name);
            }
        };
    }

    @Override
    protected PropertyConversion<?, ?> newMultiplicityOneConversion(AbstractProperty abstractProperty, Conversion<?, ?> conversion, Type type)
    {
        return new JsonDeserializationMultiplicityOne(abstractProperty, abstractProperty._owner() instanceof Association, conversion, type);
    }

    @Override
    protected PropertyConversion<?, ?> newMultiplicityManyConversion(AbstractProperty abstractProperty, Conversion<?, ?> conversion, Type type)
    {
        return new JsonDeserializationMultiplicityMany(abstractProperty, abstractProperty._owner() instanceof Association, conversion, type);
    }

    @Override
    protected PropertyConversion<?, ?> newMultiplicityParameterisedConversion(AbstractProperty abstractProperty, Conversion<?, ?> conversion, Type type)
    {
        return new JsonDeserializationMultiplicityParameterised(abstractProperty, abstractProperty._owner() instanceof Association, conversion, type);
    }

    @Override
    protected PropertyConversion<?, ?> newMultiplicityOptionalConversion(AbstractProperty abstractProperty, Conversion<?, ?> conversion, Type type)
    {
        return new JsonDeserializationMultiplicityOptional(abstractProperty, abstractProperty._owner() instanceof Association, conversion, type);
    }

    @Override
    protected PropertyConversion<?, ?> newMultiplicityRangeConversion(AbstractProperty abstractProperty, Conversion<?, ?> conversion, Type type)
    {
        return new JsonDeserializationMultiplicityRange(abstractProperty, abstractProperty._owner() instanceof Association, conversion, type, abstractProperty._multiplicity());
    }

    @Override
    public T apply(Object input, ConversionContext context)
    {
        if (input instanceof JSONObject)
        {
            JsonDeserializationContext deserializationContext = (JsonDeserializationContext)context;
            ObjectFactory objectFactory = deserializationContext.getObjectFactory();

            JSONObject jsonObject = (JSONObject)input;
            Map<String, RichIterable<?>> propertyKeyValues = this.keyValueProperties(jsonObject, deserializationContext);
            try
            {
                return (T)objectFactory.newObject(this.clazz, propertyKeyValues);
            }
            catch (PureExecutionException e)
            {
                throw new PureExecutionException(deserializationContext.getSourceInformation(), "Could not create new instance of " + this.pureTypeAsString() + ": \n" + e.getInfo());
            }
        }
        else
        {
            // value is a not a JSON object but number or string, a reference to another object.
            // Currently this should be ignored.
            return null;
        }
    }

    /**
     * Generates a mapping of property name to a collection of values which describe the properties of the newly instantiated object.
     */
    private Map<String, RichIterable<?>> keyValueProperties(JSONObject jsonObject, JsonDeserializationContext context)
    {
        this.failOnUnknownProperties(jsonObject, context);
        Map<String, RichIterable<?>> keyValues = new HashMap<>();
        for (Conversion<?, ?> propertyConversion : this.propertyConversions)
        {
            JsonPropertyDeserialization<?> jsonPropertyDeserialization = (JsonPropertyDeserialization)propertyConversion;
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
                throw new PureExecutionException(context.getSourceInformation(), "Error populating property '" + jsonPropertyDeserialization.getName() + "' on class '" + this.pureTypeAsString() + "': \n" + e.getInfo());
            }
            catch (ClassCastException | IllegalArgumentException e)
            {
                String foundType = jsonValue instanceof JSONObject ? "JSON Object" : PrimitiveConversion.toPurePrimitiveName(jsonValue.getClass());
                throw new PureExecutionException(context.getSourceInformation(), "Error populating property '" + jsonPropertyDeserialization.getName() + "' on class '" + this.pureTypeAsString() + "': \nExpected " + jsonPropertyDeserialization.pureTypeAsString() + ", found " + foundType);
            }
        }
        return keyValues;
    }

    /**
     * If the failOnUnknownProperties flag is set in the JSONDeserializationConfig (and as a result in the JsonDeserializationContext),
     * then ensure that no additional data is provided in the JSONObjects modelling instances of PURE classes.
     * This is relatively expensive to compute but the flag is rarely set.
     */
    private void failOnUnknownProperties(JSONObject jsonValue, JsonDeserializationContext context)
    {
        if (context.isFailOnUnknownProperties())
        {
            RichIterable<String> unknownProperties = LazyIterate.select((Set<String>)jsonValue.keySet(), this.isUnknownJsonProperty);
            if (!unknownProperties.isEmpty())
            {
                StringBuilder errorMsg = new StringBuilder();
                for (String unknownProperty : unknownProperties)
                {
                    errorMsg.append(String.format("Property '%s' can't be found in class %s. ", unknownProperty, PackageableElement.getUserPathForPackageableElement(this.clazz)));
                }
                throw new PureExecutionException(context.getSourceInformation(), errorMsg.toString());
            }
        }
    }
}
