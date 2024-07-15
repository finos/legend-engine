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

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Generalization;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.ConversionCache;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * Utility class for parsing JSON
 */
public class JsonDeserializer
{
    /**
     * If the passed object extracted from the JSON is a JSONObject (i.e. describes a class) then any ambiguity in which
     * class is being described, typically because the model simply states the type as some superclass, is resolved by
     * using the TypeKey property of the JSONObject. This property has a default key of "@type", but can be configured
     * in the JSON Deserialization Config, and is then stored in the JsonConverterContext when fromJson is called.
     *
     * @param typeFromPropertyDefinition The assumed type from either being passed into the fromJson call, or from the
     *                                   model on recursive calls.
     * @param obj                        The Object derived from the JSON; either some native type, JSONArray, or JSONObject.
     * @param typeKeyName                Override of the TypeKey property.
     */
    static Type resolveType(Type typeFromPropertyDefinition, Object obj, String typeKeyName, Map<String, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class> lookup, SourceInformation si)
    {
        if (!(obj instanceof JSONObject) || !(typeFromPropertyDefinition instanceof Class))
        {
            return typeFromPropertyDefinition;
        }
        JSONObject jsonObject = (JSONObject) obj;
        Class<?> classFromPropertyDefinition = (Class<?>) typeFromPropertyDefinition;
        String specifiedType = (String) jsonObject.get(typeKeyName);
        if (lookup != null && lookup.get(specifiedType) != null)
        {
            return lookup.get(specifiedType);
        }
        if (typeKeyName == null || specifiedType == null || classFromPropertyDefinition.getName().equals(specifiedType))
        {
            return classFromPropertyDefinition;
        }
        Deque<Generalization> deque = new ArrayDeque<>(classFromPropertyDefinition._specializations().toSet());
        MutableSet<Generalization> set = Sets.mutable.ofAll(classFromPropertyDefinition._specializations());
        while (!deque.isEmpty())
        {
            Type type = deque.poll()._specific();
            if (specifiedType.equals(type.getName()))
            {
                return type;
            }
            for (Generalization g : type._specializations())
            {
                if (!set.contains(g))
                {
                    set.add(g);
                    deque.addLast(g);
                }
            }
        }
        throw new PureExecutionException(si, String.format("Could not find a sub-type of \"%s\" with name \"%s\".", PackageableElement.getUserPathForPackageableElement(classFromPropertyDefinition), specifiedType));
    }

    @SuppressWarnings("unchecked")
    // as resolvedType is a Class (because all root JSON structures must be a PURE class), only ClassConverters, or in case of a Map, MapConverters, can be returned by the ConverterCache
    public static <T extends Any> T fromJson(String json, Class<T> clazz, JsonDeserializationContext context)
    {
        try
        {
            ConversionCache cache = new JsonDeserializationCache();
            JSONObject jsonObject = (JSONObject) new JSONParser().parse(json);
            Class<?> resolvedType = (Class<?>) resolveType(clazz, jsonObject, context.getTypeKeyName(), context.getTypeLookup(), context.getSourceInformation());
            if (resolvedType.getName().equals("Map"))
            {
                JsonMapDeserialization<T> conversion = (JsonMapDeserialization<T>) cache.getConversion(resolvedType, context); // Map is of Class type but has no properties, thus using a separate deserialization strategy
                return conversion.apply(jsonObject, context);
            }
            else
            {
                JsonClassDeserialization<T> conversion = (JsonClassDeserialization<T>) cache.getConversion(resolvedType, context);
                return conversion.apply(jsonObject, context);
            }
        }
        catch (ParseException e)
        {
            throw new IllegalArgumentException("Illegal JSON.");
        }
        catch (ClassCastException e)
        {
            String java11runtime = "class org.json.simple.JSONArray cannot be cast to class org.json.simple.JSONObject (org.json.simple.JSONArray and org.json.simple.JSONObject are in unnamed module of loader 'app')";
            String java7runtime = "org.json.simple.JSONArray cannot be cast to org.json.simple.JSONObject";
            if (java7runtime.equals(e.getMessage()) || java11runtime.equals(e.getMessage()))
            {
                throw new PureExecutionException(context.getSourceInformation(), "Can only deserialize root-level JSONObjects i.e. serialized single instances of PURE classes. Cannot deserialize collections of multiple PURE objects.");
            }
            else
            {
                throw e;
            }
        }
    }
}
