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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.measure.Measure;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.ConversionContext;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.UnitConversion;

public class JsonUnitSerialization<T extends CoreInstance> extends UnitConversion<T, Object>
{
    public JsonUnitSerialization(CoreInstance type)
    {
        super(type);
    }

    @Override
    public Object apply(T pureObject, ConversionContext context)
    {
        InstanceValue unitInstance = (InstanceValue) pureObject;
        return Maps.mutable.with(
                UNIT_KEY_NAME, Lists.mutable.<Object>with(Maps.mutable.with(UNIT_ID_KEY_NAME, getUnitId(unitInstance), EXPONENT_VALUE_KEY_NAME, 1)),
                VALUE_KEY_NAME, getValue(unitInstance));
    }

    private String getUnitId(InstanceValue unitInstance)
    {
        Type type = unitInstance._genericType()._rawType();
        return (type instanceof Unit) ? Measure.getUserPathForUnit(type) : PackageableElement.getUserPathForPackageableElement(type);
    }

    private Number getValue(InstanceValue unitInstance)
    {
        Object value = unitInstance._values().getAny();
        if (value instanceof Number)
        {
            return (Number) value;
        }
        if (value instanceof InstanceValue)
        {
            return (Number) ((InstanceValue) value)._values().getAny();
        }
        throw new PureExecutionException("Unexpected unit value: " + value);
    }
}
