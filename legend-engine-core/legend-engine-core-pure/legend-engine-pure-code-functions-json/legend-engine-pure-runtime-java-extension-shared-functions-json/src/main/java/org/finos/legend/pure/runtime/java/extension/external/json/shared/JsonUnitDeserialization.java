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
        JSONArray unitsJsonArray;
        try
        {
            unitsJsonArray = (JSONArray) ((JSONObject) value).get(UNIT_KEY_NAME);
            unitTypeString = ((JSONObject) unitsJsonArray.get(0)).get(UNIT_ID_KEY_NAME).toString();
            unitTypeExponent = (Number) ((JSONObject) unitsJsonArray.get(0)).get(EXPONENT_VALUE_KEY_NAME);
        }
        catch (Exception e)
        {
            throw new PureExecutionException("Mal-formatted Json for unit.", e);
        }
        if (unitsJsonArray.size() != 1)
        {
            throw new PureExecutionException("Currently composite units are not supported.");
        }
        if (unitTypeExponent.intValue() != 1)
        {
            throw new PureExecutionException("Currently non-one exponent for unit is not supported. Got: " + unitTypeExponent + ".");
        }
        Number unitValue;
        try
        {
            unitValue = (Number) ((JSONObject) value).get(VALUE_KEY_NAME);
        }
        catch (ClassCastException e)
        {
            throw new PureExecutionException("Value from unitValue field must be of Number type, getting " + ((JSONObject) value).get(VALUE_KEY_NAME).getClass().getName() + " type instead.", e);
        }

        JsonDeserializationContext deserializationContext = (JsonDeserializationContext) context;
        try
        {
            return deserializationContext.getObjectFactory().newUnitInstance(this.type, unitTypeString, unitValue);
        }
        catch (Exception e)
        {
            throw new PureExecutionException(deserializationContext.getSourceInformation(), "Could not create new instance of " + this.pureTypeAsString(), e);
        }
    }
}
