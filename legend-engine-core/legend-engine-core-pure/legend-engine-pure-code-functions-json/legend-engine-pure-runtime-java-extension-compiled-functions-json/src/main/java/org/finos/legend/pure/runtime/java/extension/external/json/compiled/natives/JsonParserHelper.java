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

package org.finos.legend.pure.runtime.java.extension.external.json.compiled.natives;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.eclipse.collections.api.factory.Stacks;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ConstraintsOverride;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateTime;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPurePrimitiveTypeMapping;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.FullJavaPaths;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataAccessor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

/* Helper class use by the generated code to deserialize JSON.
 */
@SuppressWarnings("unused")
public class JsonParserHelper
{
    public static final String JSON_FACTORY = "_JsonFactory";

    /**
     * Ensures a given JSON array meets the multiplicity requirements of the PURE class.
     *
     * @param array      The JSON array in question.
     * @param lowerBound The lower bound of the property's multiplicity
     * @param upperBound The upper bound of the property's multiplicity, -1 if multiplicity has no upper bound
     */
    public static void multiplicityIsInRange(JSONArray array, int lowerBound, int upperBound, String humanReadableMultiplicity, SourceInformation si)
    {
        boolean outsideLowerBound = array.size() < lowerBound;
        boolean outsideUpperBound = upperBound != -1 && array.size() > upperBound;

        if (outsideLowerBound || outsideUpperBound)
        {
            throw new PureExecutionException(si, "Expected value(s) of multiplicity " + humanReadableMultiplicity + ", found " + array.size() + " value(s).", Stacks.mutable.empty());
        }
    }

    /**
     * Checks the multiplicity of a "to one" property field.
     *
     * @param object     The object in question, null if does not occur in source JSONObject
     * @param lowerBound The lower bound of the property's multiplicity
     * @param upperBound The upper bound of the property's multiplicity, -1 if the multiplicity has no upper bound
     */
    public static void multiplicityIsInRange(Object object, int lowerBound, int upperBound, String humanReadableMultiplicity, SourceInformation si)
    {
        boolean outsideLowerBound = (object == null && lowerBound == 1) || lowerBound > 1;
        boolean outsideUpperBound = object != null && upperBound == 0;

        String errorMsg = object == null ? "0" : "1";

        if (outsideLowerBound || outsideUpperBound)
        {
            throw new PureExecutionException(si, "Expected value(s) of multiplicity " + humanReadableMultiplicity + ", found " + errorMsg + " value(s).", Stacks.mutable.empty());
        }
    }

    /*This method is invoked by the native function.*/
    @SuppressWarnings("unused")
    public static <T> T fromJson(String jsonString, Class<T> clazz, String fullClassName, String fullUserPath, MetadataAccessor metadata, ClassLoader classLoader,
                                 SourceInformation si, String typeKey, boolean failOnUnknownProperties, ConstraintsOverride constraintsOverride, ExecutionSupport es)
    {
        JSONParser parser = new JSONParser();
        try
        {
            JSONObject root = (JSONObject) parser.parse(jsonString);
            return fromJson(root, clazz, fullClassName, fullUserPath, metadata, classLoader, si, typeKey, failOnUnknownProperties, constraintsOverride, es, "");
        }
        catch (ParseException e)
        {
            throw new RuntimeException("Unable to parse json string " + jsonString, e);
        }
    }

    /**
     * Checks that the value being extracted from the JSON (actualValue) is of the expected type.
     *
     * @param actualValue      The value extracted from the JSON.
     * @param expectedJavaType The expected Java type of the value.
     * @param expectedPureType The expected Pure type.
     * @param sourceInfo       Source information for an exception message.
     * @throws PureExecutionException Exception information describes property on class being assigned, as well as the expected and actual types. For example,
     *                                "Error populating property 'testField' on class 'meta::pure::functions::json::tests::testClass': Expected class java.lang.Boolean, found String"
     */
    private static void typeCheck(Object actualValue, Class<?> expectedJavaType, String expectedPureType, SourceInformation sourceInfo)
    {
        if (!expectedJavaType.isInstance(actualValue))
        {
            String actualType;
            if (actualValue instanceof Map)
            {
                actualType = "JSON object";
            }
            else if (actualValue instanceof List)
            {
                actualType = "JSON array";
            }
            else
            {
                actualType = JavaPurePrimitiveTypeMapping.getPureM3TypeFromJavaPrimitivesAndDates(actualValue);
                if (actualType == null)
                {
                    actualType = actualValue.getClass().getSimpleName();
                }
            }
            throw new PureExecutionException("Expected " + expectedPureType + ", found " + actualType, Stacks.mutable.empty());
        }
    }

    @SuppressWarnings({"unused", "unchecked"})
    public static <T> T fromJson(Object value, Class<T> resultType, String fullClassName, String fullUserPath, MetadataAccessor metadata, ClassLoader classLoader, SourceInformation si, String typeKey, boolean failOnUnknownProperties, ConstraintsOverride constraintsOverride, ExecutionSupport es, String parentClass)
    {
        if (value == null)
        {
            return null;
        }

        // Is value a Primitive type?
        if (String.class.equals(resultType))
        {
            typeCheck(value, String.class, fullClassName, si);
            return (T) jsonToString(value);
        }
        if (Integer.class.equals(resultType))
        {
            typeCheck(value, Integer.class, fullClassName, si);
            return (T) jsonToInteger(value);
        }
        if (Long.class.equals(resultType))
        {
            typeCheck(value, Long.class, fullClassName, si);
            return (T) jsonToLong(value);
        }
        if (Boolean.class.equals(resultType))
        {
            typeCheck(value, Boolean.class, fullClassName, si);
            return (T) jsonToBoolean(value);
        }
        if (Double.class.equals(resultType))
        {
            typeCheck(value, Number.class, fullClassName, si);
            return (T) jsonToDouble(value);
        }
        // Is value a Date ?
        if (PureDate.class.isAssignableFrom(resultType))
        {
            typeCheck(value, String.class, fullClassName, si);
            return (T) DateFunctions.parsePureDate((String) value);
        }
        if (DateTime.class.isAssignableFrom(resultType))
        {
            typeCheck(value, String.class, fullClassName, si);
            return (T) DateFunctions.parsePureDate((String) value);
        }
        // Is value an Enum type?
        try
        {
            if (getPureEnumParent(resultType).isAssignableFrom(resultType))
            {
                typeCheck(value, String.class, fullClassName, si);

                String enumString = (String) value;
                int dotIndex = enumString.lastIndexOf('.');
                String enumName;
                String enumerationFullUserPath = fullUserPath;
                if (dotIndex == -1)
                {
                    enumName = enumString;
                }
                else
                {
                    enumName = enumString.substring(dotIndex + 1);
                    enumerationFullUserPath = enumString.substring(0, dotIndex);
                    if (!fullClassName.regionMatches(5, enumerationFullUserPath.replace("::", "_"), 0, fullClassName.length() - 5))
                    {
                        throw new PureExecutionException(si, "Expected enum of type " + fullClassName + "; got: " + enumString, Stacks.mutable.empty());
                    }
                }
                T result = (T) metadata.getEnum(enumerationFullUserPath, enumName);
                if (result == null)
                {
                    throw new PureExecutionException(si, "Unknown enum: " + fullClassName + "." + enumName, Stacks.mutable.empty());
                }
                return result;
            }
        }
        catch (NullPointerException e)
        {
            System.out.println("Unable to read the value of [" + value + "] because of NullPointerException");
            return null;
        }

        if (!(value instanceof JSONObject))
        {
            //value is a not a JSON object but number or string, a reference to another object.
            // Currently this should be ignored.
            return null;
        }

        JSONObject jsonObject = (JSONObject) value;
        try
        {
            Class<?> implementationClass = resolveFactoryClass(resultType);
            return (T) MethodUtils.invokeExactStaticMethod(implementationClass, "fromJson",
                    new Object[]{value, metadata, classLoader, si, typeKey, failOnUnknownProperties, constraintsOverride, es, parentClass}, new Class[]{JSONObject.class, MetadataAccessor.class, ClassLoader.class, SourceInformation.class, String.class, boolean.class, ConstraintsOverride.class, ExecutionSupport.class, String.class});
        }
        catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException e)
        {
            throw new RuntimeException("Unable to call fromJson() method", e);
        }
        catch (InvocationTargetException e)
        {
            Throwable targetException = e.getTargetException();
            if (targetException instanceof RuntimeException)
            {
                throw (RuntimeException) targetException;
            }
            throw new RuntimeException("Error calling fromJSON() method", e);
        }
    }

    /*
     *
     * @return Base class for enumeration. It is used to see if current type is an enum.
     */
    private static Class<?> getPureEnumParent(Class<?> resultType)
    {
        Class<?> result;
        try
        {
            result = resultType.getClassLoader().loadClass(FullJavaPaths.Enum);
        }
        catch (ClassNotFoundException e)
        {
            throw new IllegalStateException("Unable to load PURE enum parent", e);
        }

        return result;
    }

    /* Helpers for standard java types*/
    private static String jsonToString(Object value)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof String)
        {
            return (String) value;
        }
        return value.toString();
    }

    private static Integer jsonToInteger(Object value)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof Integer)
        {
            return (Integer) value;
        }
        return Integer.parseInt(value.toString());
    }

    private static Long jsonToLong(Object value)
    {
        if (value == null)
        {
            return null;
        }

        if (value instanceof Long)
        {
            return (Long) value;
        }
        return Long.parseLong(value.toString());
    }

    private static Double jsonToDouble(Object value)
    {
        // Some double values are being parsed as long by the parser
        if (value == null)
        {
            return null;
        }
        if (value instanceof Number)
        {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(value.toString());
    }

    private static Boolean jsonToBoolean(Object value)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof Boolean)
        {
            return (Boolean) value;
        }
        return Boolean.valueOf(value.toString());
    }

    /*The generated factory has the same name as the class with a suffix _JsonFactory.
     * Use the same classloader to load the factory class.*/
    private static Class<?> resolveFactoryClass(Class<?> typeFromClassMetadata) throws ClassNotFoundException
    {
        ClassLoader classLoader = typeFromClassMetadata.getClassLoader();
        String defaultFactory = typeFromClassMetadata.getName() + JSON_FACTORY;
        return classLoader.loadClass(defaultFactory);
    }
}
