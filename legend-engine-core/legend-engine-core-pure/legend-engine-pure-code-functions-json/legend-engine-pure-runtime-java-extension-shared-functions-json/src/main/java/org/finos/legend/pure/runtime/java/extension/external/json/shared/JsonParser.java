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

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Set;

public final class JsonParser
{
    public static JsonExtraTypeProcessor processor = new DefaultJsonExtraTypeProcessor();
    private static final String JSONKeyValue = "meta::json::JSONKeyValue";
    private static final String JSONString = "meta::json::JSONString";
    private static final String JSONNumber = "meta::json::JSONNumber";
    private static final String JSONBoolean = "meta::json::JSONBoolean";
    private static final String JSONArray = "meta::json::JSONArray";
    private static final String JSONObject = "meta::json::JSONObject";
    private static final String JSONNull = "meta::json::JSONNull";

    private static final String key = "key";
    private static final String value = "value";
    private static final String values = "values";
    private static final String keyValuePairs = "keyValuePairs";
    private final ProcessorSupport processorSupport;


    public JsonParser(ProcessorSupport processorSupport)
    {
        this.processorSupport = processorSupport;
    }

    public CoreInstance toPureJson(String input)
    {
        try
        {
            Object object = new JSONParser().parse(input);
            return this.createJsonElement(object);
        }
        catch (ParseException e)
        {
            throw new RuntimeException("Failed to parse input: " + e.getMessage(), e);
        }
    }

    public CoreInstance toPureJson(Reader input) throws IOException
    {
        try
        {
            Object object = new JSONParser().parse(input);
            return this.createJsonElement(object);
        }
        catch (ParseException e)
        {
            throw new RuntimeException("Failed to parse input: " + e.getMessage(), e);
        }
    }

    public CoreInstance createJsonElement(Object object)
    {
        if (object == null)
        {
            return this.createJsonNull();
        }
        else if (object instanceof org.json.simple.JSONObject)
        {
            return this.createJsonObject((org.json.simple.JSONObject)object);
        }
        else if (object instanceof String)
        {
            return this.createJsonString((String)object);
        }
        else if (object instanceof Number)
        {
            return this.createJsonNumber((Number)object);
        }
        else if (object instanceof Boolean)
        {
            return this.createJsonBoolean((Boolean)object);
        }
        else if (object instanceof org.json.simple.JSONArray)
        {
            return this.createJsonArray((org.json.simple.JSONArray)object);
        }
        else
        {
            throw new RuntimeException("Unexpected JSON Element with class " + object.getClass().getName());
        }
    }

    private CoreInstance createJsonArray(org.json.simple.JSONArray jsonArray)
    {
        MutableList<CoreInstance> results = Lists.mutable.of();

        for (Object val : jsonArray)
        {
            results.add(this.createJsonElement(val));
        }

        CoreInstance instance = this.createCoreInstance(JSONArray);
        Instance.setValuesForProperty(instance, values, results, this.processorSupport);
        return instance;
    }

    private CoreInstance createJsonObject(org.json.simple.JSONObject jsonObject)
    {
        MutableList<CoreInstance> results = Lists.mutable.of();

        for (Map.Entry entry : ((Set<Map.Entry>)jsonObject.entrySet()))
        {
            String entryKey = (String)entry.getKey();

            CoreInstance jsonKeyValue = this.createCoreInstance(JSONKeyValue);
            Instance.setValuesForProperty(jsonKeyValue, key, Lists.immutable.of(this.createJsonString(entryKey)), this.processorSupport);
            Instance.setValuesForProperty(jsonKeyValue, value, Lists.immutable.of(this.createJsonElement(entry.getValue())), this.processorSupport);
            results.add(jsonKeyValue);
        }

        CoreInstance instance = this.createCoreInstance(JSONObject);
        Instance.setValuesForProperty(instance, keyValuePairs, results, this.processorSupport);
        return instance;
    }


    private CoreInstance createJsonString(String value)
    {
        CoreInstance coreInstance = this.createCoreInstance(JSONString);
        CoreInstance stringInstance = this.processorSupport.newCoreInstance(value, M3Paths.String, null);
        Instance.setValuesForProperty(coreInstance, M3Properties.value, Lists.immutable.of(stringInstance), this.processorSupport);
        return coreInstance;
    }


    private CoreInstance createJsonNumber(Number value)
    {
        ListIterable<? extends Class> integerTypes = FastList.newListWith(Integer.class, Long.class, BigInteger.class);
        ListIterable<? extends Class> floatTypes = FastList.newListWith(Float.class, Double.class, BigDecimal.class);

        String m3NumberType;

        if (integerTypes.anySatisfy(Predicates.equal(value.getClass())))
        {
            m3NumberType = M3Paths.Integer;
        }
        else if (floatTypes.anySatisfy(Predicates.equal(value.getClass())))
        {
            m3NumberType = M3Paths.Float;
        }
        else
        {
            throw new RuntimeException("Json value is not compatible with supported primitive types: Integer Types: " + integerTypes + " Float Types: " + floatTypes);
        }

        CoreInstance coreInstance = this.createCoreInstance(JSONNumber);
        CoreInstance numberInstance = this.processorSupport.newCoreInstance(value.toString(), m3NumberType, null);
        Instance.setValuesForProperty(coreInstance, M3Properties.value, Lists.immutable.of(numberInstance), this.processorSupport);
        return coreInstance;
    }

    private CoreInstance createJsonBoolean(Boolean value)
    {
        CoreInstance coreInstance = this.createCoreInstance(JSONBoolean);
        CoreInstance booleanInstance = this.processorSupport.newCoreInstance(value.toString(), M3Paths.Boolean, null);
        Instance.setValuesForProperty(coreInstance, M3Properties.value, Lists.immutable.of(booleanInstance), this.processorSupport);
        return coreInstance;
    }

    private CoreInstance createJsonNull()
    {
        return this.createCoreInstance(JSONNull);
    }

    private CoreInstance createCoreInstance(String type)
    {
        ProcessorSupport processorSupport = this.processorSupport;
        return processorSupport.newEphemeralAnonymousCoreInstance(type);
    }

}
