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
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.ClassConversion;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.Conversion;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.ConversionContext;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.PropertyConversion;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class JsonClassSerialization<T extends CoreInstance> extends ClassConversion<T, Object>
{
    protected RichIterable<JsonQualifiedPropertySerialization<?>> qualifiedPropertySerializations;

    public JsonClassSerialization(Class<?> clazz)
    {
        super(clazz);
    }

    @Override
    protected PropertyConversion<?, ?> newMultiplicityOneConversion(AbstractProperty abstractProperty, Conversion<?, ?> conversion, Type type)
    {
        return new JsonSerializationMultiplicityOne(abstractProperty, abstractProperty._owner() instanceof Association, conversion, type);
    }

    @Override
    protected PropertyConversion<?, ?> newMultiplicityManyConversion(AbstractProperty abstractProperty, Conversion<?, ?> conversion, Type type)
    {
        return new JsonSerializationMultiplicityMany(abstractProperty, abstractProperty._owner() instanceof Association, conversion, type);
    }

    @Override
    protected PropertyConversion<?, ?> newMultiplicityParameterisedConversion(AbstractProperty abstractProperty, Conversion<?, ?> conversion, Type type)
    {
        return new JsonSerializationMultiplicityParameterised(abstractProperty, abstractProperty._owner() instanceof Association, conversion, type);
    }

    @Override
    protected PropertyConversion<?, ?> newMultiplicityOptionalConversion(AbstractProperty abstractProperty, Conversion<?, ?> conversion, Type type)
    {
        return new JsonSerializationMultiplicityOptional(abstractProperty, abstractProperty._owner() instanceof Association, conversion, type);
    }

    @Override
    protected PropertyConversion<?, ?> newMultiplicityRangeConversion(AbstractProperty abstractProperty, Conversion<?, ?> conversion, Type type)
    {
        return new JsonSerializationMultiplicityRange(abstractProperty, abstractProperty._owner() instanceof Association, conversion, type, abstractProperty._multiplicity());
    }

    @Override
    protected void completeInitialisation(ConversionContext context)
    {
        super.completeInitialisation(context);
        if (((JsonSerializationContext) context).isSerializeQualifiedProperties())
        {
            this.qualifiedPropertySerializations = this.computeQualifiedPropertyConverters((JsonSerializationContext) context);
        }
    }

    private RichIterable<JsonQualifiedPropertySerialization<?>> computeQualifiedPropertyConverters(JsonSerializationContext context)
    {
        MutableList<JsonQualifiedPropertySerialization<?>> qualifiedPropertyConversions = Lists.mutable.empty();
        RichIterable<QualifiedProperty<?>> qualifiedProperties = this.getQualifiedProperties(context.getProcessorSupport());
        for (QualifiedProperty<?> qualifiedProperty : qualifiedProperties)
        {
            FunctionType functionType = (FunctionType) context.getProcessorSupport().function_getFunctionType(qualifiedProperty);
            if (functionType._parameters().size() <= 1)
            {
                Type returnType = functionType._returnType()._rawType();
                Multiplicity returnMultiplicity = functionType._returnMultiplicity();
                Conversion<?, ?> conversion = context.getConversionCache().getConversion(returnType, context);
                Long upperBound = returnMultiplicity._upperBound()._value();
                Conversion<?, ?> multiplicityConversion;
                if (upperBound == null)
                {
                    multiplicityConversion = new JsonSerializationMultiplicityMany(qualifiedProperty, false, conversion, returnType);
                }
                else
                {
                    Long lowerBound = returnMultiplicity._lowerBound()._value();
                    if (lowerBound == 1 && upperBound == 1)
                    {
                        multiplicityConversion = new JsonSerializationMultiplicityOne(qualifiedProperty, false, conversion, returnType);
                    }
                    else if (lowerBound == 0 && upperBound == 1)
                    {
                        multiplicityConversion = new JsonSerializationMultiplicityOptional(qualifiedProperty, false, conversion, returnType);
                    }
                    else
                    {
                        multiplicityConversion = new JsonSerializationMultiplicityRange(qualifiedProperty, false, conversion, returnType, returnMultiplicity);
                    }
                }
                qualifiedPropertyConversions.add(new JsonQualifiedPropertySerialization<QualifiedProperty>(qualifiedProperty, qualifiedProperty._name(), returnType, returnMultiplicity, multiplicityConversion));
            }
        }
        return qualifiedPropertyConversions;
    }

    @Override
    public Object apply(T pureObject, ConversionContext context)
    {
        JsonSerializationContext jsonSerializationContext = (JsonSerializationContext) context;
        if (this.isExactType(pureObject, context.getProcessorSupport()))
        {
            return this.serializeProperties(pureObject, jsonSerializationContext);
        }
        else
        {
            Conversion correctTypeClassConversion = this.resolveTypeAndGetConverter(pureObject, jsonSerializationContext);
            return correctTypeClassConversion.apply(pureObject, jsonSerializationContext);
        }
    }

    private Object serializeProperties(T pureObject, JsonSerializationContext jsonSerializationContext)
    {
        if ((jsonSerializationContext.isSerializePackageableElementName() && pureObject instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement) || MetamodelSerializationOverrides.shouldSerializeAsElementToPath(pureObject))
        {
            return MetamodelSerializationOverrides.serializePackageableElement(pureObject, jsonSerializationContext.isSerializeMultiplicityAsNumber());
        }
        jsonSerializationContext.getVisitedInstances().push(pureObject);
        JSONObject json = new JSONObject();
        if (jsonSerializationContext.isIncludeType())
        {
            json.put(jsonSerializationContext.getTypeKeyName(), this.pureTypeAsString());
        }
        boolean shouldApplyMetamodelPropertyFilter = MetamodelSerializationOverrides.applyMetamodelPropertyFilter(this.clazz);
        if (shouldApplyMetamodelPropertyFilter)
        {
            this.propertyConversions = this.propertyConversions.select(MetamodelSerializationOverrides.computeMetamodelPropertyFilter(this.pureTypeAsString()));
        }
        for (PropertyConversion propertyConversion : this.propertyConversions)
        {
            Object values = jsonSerializationContext.getValueForProperty(pureObject, (Property) propertyConversion.getProperty(), this.clazz._name());
            Object serializedValues = propertyConversion.apply(values, jsonSerializationContext);
            if (this.doNotDropKeyValue(jsonSerializationContext, serializedValues))
            {
                json.put(propertyConversion.getName(), serializedValues);
            }
        }
        if (jsonSerializationContext.isSerializeQualifiedProperties())
        {
            for (JsonQualifiedPropertySerialization jsonQualifiedPropertySerialization : this.qualifiedPropertySerializations)
            {
                json.put(jsonQualifiedPropertySerialization.getName(), jsonQualifiedPropertySerialization.apply(pureObject, jsonSerializationContext));
            }
        }
        jsonSerializationContext.getVisitedInstances().pop();
        return json;
    }

    private boolean doNotDropKeyValue(JsonSerializationContext jsonSerializationContext, Object serializedValues)
    {
        return !(jsonSerializationContext.isRemovePropertiesWithEmptyValues() && serializedValues instanceof JSONArray && ((JSONArray) serializedValues).isEmpty()
                || serializedValues == JsonPropertySerialization.CYCLE_DETECTED);
    }

    private boolean isExactType(T pureObject, ProcessorSupport processorSupport)
    {
        return processorSupport.getClassifier(pureObject).equals(this.clazz);
    }

    private Conversion resolveTypeAndGetConverter(T pureObject, JsonSerializationContext jsonSerializationContext)
    {
        return jsonSerializationContext.getConversionCache().getConversion((Type) jsonSerializationContext.getProcessorSupport().getClassifier(pureObject), jsonSerializationContext);
    }
}
