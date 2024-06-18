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

import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.ConversionContext;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.ObjectFactory;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.UnitConversion;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class JsonUnitDeserialization<T extends Any> extends UnitConversion<Object, T>
{
    public JsonUnitDeserialization(CoreInstance type)
    {
        super(type);
    }

    @Override
    public T apply(Object value, ConversionContext context)
    {
        String unitTypeString;
        Number unitTypeExponent;
        boolean isComposite;
        JSONArray unitsJsonArray;
        try
        {
            unitsJsonArray = (JSONArray) ((JSONObject) value).get(this.unitKeyName);
            unitTypeString = ((JSONObject) unitsJsonArray.get(0)).get("unitId").toString();
            unitTypeExponent = (Number) ((JSONObject) unitsJsonArray.get(0)).get("exponentValue");
        }
        catch (Exception e)
        {
            throw new PureExecutionException("Mal-formatted Json for unit.");
        }
        isComposite = 1 != unitsJsonArray.size();
        if (isComposite)
        {
            throw new PureExecutionException("Currently composite units are not supported.");
        }
        if (!Long.valueOf(1).equals(unitTypeExponent))
        {
            throw new PureExecutionException("Currently non-one exponent for unit is not supported. Got: " + unitTypeExponent.toString() + ".");
        }
        Number unitValue;
        try
        {
            unitValue = (Number) ((JSONObject) value).get(this.valueKeyName);
        }
        catch (ClassCastException cce)
        {
            throw new PureExecutionException("Value from unitValue field must be of Number type, getting " + ((JSONObject) value).get(this.valueKeyName).getClass().getName() + " type instead.");
        }
        JsonDeserializationContext deserializationContext = (JsonDeserializationContext) context;
        ObjectFactory objectFactory = deserializationContext.getObjectFactory();

        try
        {
            return (T) objectFactory.newUnitInstance(this.type, unitTypeString, unitValue);
        }
        catch (Exception e)
        {
            throw new PureExecutionException(deserializationContext.getSourceInformation(), "Could not create new instance of " + this.pureTypeAsString());
        }
    }
}
