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

import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Measure;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.PrimitiveType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.util.HashMap;
import java.util.Map;

public abstract class ConversionCache
{
    protected final Map<Type, Conversion<?, ?>> cache;
    private final Map<PrimitiveConversion.PurePrimitive, PrimitiveConversion<?,?>> primitiveConversionMap;

    public ConversionCache()
    {
        this.cache = new HashMap<>();
        this.primitiveConversionMap = this.constructPrimitiveConversions();
    }

    public Conversion<?, ?> getConversion(Type type, ConversionContext context)
    {
        if (this.cache.containsKey(type))
        {
            return this.cache.get(type);
        }
        return this.newConversion(type, context);
    }

    private Conversion<?, ?> newConversion(Type type, ConversionContext context)
    {
        if (type instanceof Class)
        {
            if (type.getName().equals("Map")) // Maps do not contain properties, need to handle separately for entries' conversions
            {
                MapConversion<?, ?> mapConversion = this.newMapConversion(context);
                this.cache.put(type, mapConversion);
                return mapConversion;
            }
            else if (type.getName().equals("Any"))
            {
                Conversion<?, ?> anyTypeConversion = this.newGenericAndAnyTypeConversion(true, context);
                this.cache.put(type, anyTypeConversion);
                return anyTypeConversion;
            }
            else
            {
                ClassConversion<?, ?> classConversion = this.newClassConversion((Class)type, context);
                this.cache.put(type, classConversion);
                classConversion.completeInitialisation(context); // reasoning for this is explained at completeInitialisation's definition
                return classConversion;
            }
        }
        if (type instanceof Enumeration)
        {
            EnumerationConversion<?,?> enumerationConversion = this.newEnumerationConversion((Enumeration)type, context);
            this.cache.put(type, enumerationConversion);
            return enumerationConversion;
        }
        if (type instanceof Unit || type instanceof Measure)
        {
            UnitConversion<?, ?> unitConversion = this.newUnitConversion(type, context);
            this.cache.put(type, unitConversion);
            return unitConversion;
        }
        if (type instanceof PrimitiveType)
        {
            return this.newPrimitiveConversion((PrimitiveType)type, context);
        }
        if (type == null)
        {
            return this.newGenericAndAnyTypeConversion(false, context);
        }
        throw new IllegalArgumentException("Unknown type.");
    }

    private Conversion<?, ?> newPrimitiveConversion(PrimitiveType type, ConversionContext context)
    {
        return this.primitiveConversionMap.get(PrimitiveConversion.PurePrimitive.valueOf(type.getName()));
    }

    protected abstract Map<PrimitiveConversion.PurePrimitive, PrimitiveConversion<?,?>> constructPrimitiveConversions();

    protected abstract ClassConversion<?, ?> newClassConversion(Class type, ConversionContext context);

    protected abstract MapConversion<?, ?> newMapConversion(ConversionContext context);

    protected abstract EnumerationConversion<?, ?> newEnumerationConversion(Enumeration type, ConversionContext context);

    protected abstract UnitConversion<?, ?> newUnitConversion(CoreInstance type, ConversionContext context);

    protected abstract Conversion<?, ?> newGenericAndAnyTypeConversion(boolean isExplicitAny, ConversionContext context);
}
