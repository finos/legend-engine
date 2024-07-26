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

package org.finos.legend.pure.runtime.java.extension.external.shared.conversion;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;

public abstract class ClassConversion<F, T> implements Conversion<F, T>
{
    protected final org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class clazz;
    protected RichIterable<PropertyConversion<?, ?>> propertyConversions;

    /**
     * Serialized PURE objects do not contain the properties inherited from Any, and as such those properties should be filtered out.
     */
    private static final Predicate<Property<?, Object>> PROPERTY_FILTER = new Predicate<Property<?, Object>>()
    {
        @Override
        public boolean accept(Property<?, Object> objectProperty)
        {
            return !"meta::pure::metamodel::type::Any".equals(PackageableElement.getUserPathForPackageableElement(objectProperty._owner()));
        }
    };

    public ClassConversion(Class clazz)
    {
        this.clazz = clazz;
        this.propertyConversions = null;
    }

    @Override
    public String pureTypeAsString()
    {
        return PackageableElement.getUserPathForPackageableElement(this.clazz);
    }

    /**
     * Completes the initialisation of a ClassConversion by populating all of its properties. This cannot be done until the
     * ClassConversion has been added to the conversion cache otherwise the act of getting a conversion from the cache for a
     * property can cause an infinite loop when the type of the property is that of its owner (or some other similar recursive class structure).
     */
    protected void completeInitialisation(ConversionContext context)
    {
        this.propertyConversions = this.computePropertyConversions(context);
    }

    private RichIterable<PropertyConversion<?, ?>> computePropertyConversions(ConversionContext context)
    {
        FastList<PropertyConversion<?, ?>> propertyConversions = new FastList<>();
        RichIterable<Property<?, Object>> properties = this.getProperties(context.getProcessorSupport()).select(PROPERTY_FILTER);
        for (Property property : properties)
        {
            Type type = property._genericType()._rawType();
            Conversion<?, ?> conversion = context.getConversionCache().getConversion(type, context);

            if (property._multiplicity()._multiplicityParameter() != null)
            {
                propertyConversions.add(this.newMultiplicityParameterisedConversion(property, conversion, type));
            }
            else
            {
                Long upperBound = property._multiplicity()._upperBound()._value();
                if (upperBound == null)
                {
                    propertyConversions.add(this.newMultiplicityManyConversion(property, conversion, type));
                }
                else
                {
                    Long lowerBound = property._multiplicity()._lowerBound()._value();
                    if (lowerBound == 1 && upperBound == 1)
                    {
                        propertyConversions.add(this.newMultiplicityOneConversion(property, conversion, type));
                    }
                    else if (lowerBound == 0 && upperBound == 1)
                    {
                        propertyConversions.add(this.newMultiplicityOptionalConversion(property, conversion, type));
                    }
                    else
                    {
                        propertyConversions.add(this.newMultiplicityRangeConversion(property, conversion, type));
                    }
                }
            }
        }
        return propertyConversions;
    }

    protected RichIterable<Property<?, Object>> getProperties(ProcessorSupport processorSupport)
    {
        return (RichIterable)processorSupport.class_getSimpleProperties(this.clazz);
    }

    protected RichIterable<QualifiedProperty<?>> getQualifiedProperties(ProcessorSupport processorSupport)
    {
        return (RichIterable)processorSupport.class_getQualifiedProperties(this.clazz);
    }

    protected abstract PropertyConversion<?, ?> newMultiplicityOneConversion(AbstractProperty abstractProperty, Conversion<?, ?> conversion, Type type);

    protected abstract PropertyConversion<?, ?> newMultiplicityManyConversion(AbstractProperty abstractProperty, Conversion<?, ?> conversion, Type type);

    protected abstract PropertyConversion<?, ?> newMultiplicityParameterisedConversion(AbstractProperty abstractProperty, Conversion<?, ?> conversion, Type type);

    protected abstract PropertyConversion<?, ?> newMultiplicityOptionalConversion(AbstractProperty abstractProperty, Conversion<?, ?> conversion, Type type);

    protected abstract PropertyConversion<?, ?> newMultiplicityRangeConversion(AbstractProperty abstractProperty, Conversion<?, ?> conversion, Type type);
}
