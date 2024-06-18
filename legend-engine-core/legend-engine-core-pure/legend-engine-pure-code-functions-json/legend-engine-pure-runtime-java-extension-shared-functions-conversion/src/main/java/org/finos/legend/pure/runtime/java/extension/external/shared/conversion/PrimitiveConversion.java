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

import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;

import java.util.HashMap;
import java.util.Map;

public abstract class PrimitiveConversion<F, T> implements Conversion<F, T>
{
    public enum PurePrimitive
    {
        String, Boolean, Integer, Float, Decimal, Number, Date, DateTime, StrictDate, LatestDate
    }

    private static final Map<Class, String> TO_PURE_PRIMITIVE_NAME_MAPPING = initToPurePrimitiveNameMapping();

    private static Map<Class, String> initToPurePrimitiveNameMapping()
    {
        Map<Class, String> map = new HashMap<>();
        map.put(String.class, M3Paths.String);
        map.put(Boolean.class, M3Paths.Boolean);
        map.put(boolean.class, M3Paths.Boolean);
        map.put(Long.class, M3Paths.Integer);
        map.put(long.class, M3Paths.Integer);
        map.put(Double.class, M3Paths.Float);
        map.put(double.class, M3Paths.Float);
        map.put(Number.class, M3Paths.Number);
        return map;
    }

    public static String toPurePrimitiveName(Class javaClass)
    {
        String purePrimitiveName = TO_PURE_PRIMITIVE_NAME_MAPPING.get(javaClass);
        if (purePrimitiveName == null)
        {
            throw new IllegalArgumentException("Unknown primitive: " + javaClass);
        }
        return purePrimitiveName;
    }

    protected String potentiallyFormatDateTime(PureDate pureObject, String format)
    {
        return format != null ? pureObject.format(format) : pureObject.toString();
    }

    /**
     * Generates a primitive conversion that does nothing but checks that the passed data is of the correct type
     */
    public static <T> PrimitiveConversion<T, T> noOpConversion(final java.lang.Class<T> primitiveJavaClass, final String pureTypeName, final boolean validateType)
    {
        return new PrimitiveConversion<T, T>()
        {
            @Override
            public T apply(T value, ConversionContext context)
            {
                if (validateType && !primitiveJavaClass.isInstance(value))
                {
                    throw new IllegalArgumentException("Invalid value.");
                }
                return value;
            }

            @Override
            public String pureTypeAsString()
            {
                return pureTypeName;
            }
        };
    }
}
