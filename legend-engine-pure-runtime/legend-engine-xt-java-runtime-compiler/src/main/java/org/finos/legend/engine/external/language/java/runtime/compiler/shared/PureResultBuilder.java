//  Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.external.language.java.runtime.compiler.shared;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.Map;

class PureResultBuilder
{
    private static final String COMP_RESULT_CLASS = "meta::external::language::java::compiler::CompilationResult";
    private static final String EXEC_RESULT_CLASS = "meta::external::language::java::compiler::ExecutionResult";
    private static final String COMP_AND_EXEC_RESULT_CLASS = "meta::external::language::java::compiler::CompileAndExecuteResult";

    private static final String JAVA_NULL_CLASS = "meta::external::language::java::compiler::JavaNull";
    private static final String JAVA_ARRAY_CLASS = "meta::external::language::java::compiler::JavaArray";
    private static final String JAVA_ENUM_CLASS = "meta::external::language::java::compiler::JavaEnum";
    private static final String JAVA_PRIMITIVE_CLASS = "meta::external::language::java::compiler::JavaPrimitive";
    private static final String JAVA_ITERABLE_CLASS = "meta::external::language::java::compiler::JavaIterable";
    private static final String JAVA_MAP_CLASS = "meta::external::language::java::compiler::JavaMap";
    private static final String JAVA_OBJECT_CLASS = "meta::external::language::java::compiler::JavaObject";

    private final ProcessorSupport processorSupport;

    PureResultBuilder(ProcessorSupport processorSupport)
    {
        this.processorSupport = processorSupport;
    }

    CoreInstance buildCompilationResult(CompilationResult javaCompResult)
    {
        CoreInstance pureCompResult = newCoreInstance(COMP_RESULT_CLASS);

        CoreInstance success = newBooleanCoreInstance(javaCompResult.isSuccess());
        setProperty(pureCompResult, "successful", success);

        if (!javaCompResult.isSuccess())
        {
            ListIterable<CoreInstance> errorMessages = javaCompResult.getErrorMessages().collect(this::newStringCoreInstance);
            setProperty(pureCompResult, "errors", errorMessages);
        }

        return pureCompResult;

    }

    CoreInstance buildExecutionResult(ExecutionResult javaExecResult)
    {
        CoreInstance pureExecResult = newCoreInstance(EXEC_RESULT_CLASS);

        CoreInstance success = newBooleanCoreInstance(javaExecResult.isSuccess());
        setProperty(pureExecResult, "successful", success);

        if (javaExecResult.isSuccess())
        {
            CoreInstance convertedResult = buildJavaValue(javaExecResult.getResult());
            setProperty(pureExecResult, "returnValue", convertedResult);
        }
        else
        {
            Throwable error = javaExecResult.getError();
            if (error != null)
            {
                StringWriter stringWriter = new StringWriter();
                try (PrintWriter printWriter = new PrintWriter(stringWriter))
                {
                    error.printStackTrace(printWriter);
                }
                CoreInstance errorMessage = newStringCoreInstance(stringWriter.toString());
                setProperty(pureExecResult, "error", errorMessage);
            }
        }

        return pureExecResult;
    }

    CoreInstance buildCompileAndExecuteResult(CompilationResult javaCompResult, ExecutionResult javaExecResult)
    {
        CoreInstance pureCompAndExecResult = newCoreInstance(COMP_AND_EXEC_RESULT_CLASS);

        CoreInstance pureCompResult = buildCompilationResult(javaCompResult);
        setProperty(pureCompAndExecResult, "compilationResult", pureCompResult);

        if (javaExecResult != null)
        {
            CoreInstance pureExecResult = buildExecutionResult(javaExecResult);
            setProperty(pureCompAndExecResult, "executionResult", pureExecResult);
        }
        return pureCompAndExecResult;
    }

    CoreInstance buildJavaValue(Object object)
    {
        if (object == null)
        {
            return buildJavaNull();
        }

        Class<?> objectClass = object.getClass();

        if (objectClass.isArray())
        {
            return buildJavaArray(object);
        }

        if (objectClass.isPrimitive())
        {
            return buildJavaPrimitive(objectClass, object);
        }

        if (objectClass.isEnum())
        {
            return buildJavaEnum((Enum<?>) object);
        }

        if (object instanceof Map)
        {
            return buildJavaMap((Map<?, ?>) object);
        }

        if (object instanceof Iterable)
        {
            return buildJavaIterable((Iterable<?>) object);
        }

        return buildJavaOtherObject(object);
    }

    private CoreInstance buildJavaNull()
    {
        return newCoreInstance("null", JAVA_NULL_CLASS);
    }

    private CoreInstance buildJavaArray(Object array)
    {
        Class<?> componentType = array.getClass().getComponentType();

        int length = Array.getLength(array);
        MutableList<CoreInstance> values = Lists.mutable.ofInitialCapacity(length);
        for (int i = 0; i < length; i++)
        {
            Object javaValue = Array.get(array, i);
            CoreInstance convertedValue = componentType.isPrimitive() ? buildJavaPrimitive(componentType, javaValue) : buildJavaValue(javaValue);
            values.add(convertedValue);
        }
        CoreInstance instance = newCoreInstance(JAVA_ARRAY_CLASS);
        setProperty(instance, "componentType", componentType.getName());
        setProperty(instance, M3Properties.values, values);
        return instance;
    }

    private CoreInstance buildJavaPrimitive(Class<?> primitiveType, Object value)
    {
        CoreInstance instance = newCoreInstance(JAVA_PRIMITIVE_CLASS);
        setProperty(instance, M3Properties.type, primitiveType.getName());
        setProperty(instance, M3Properties.value, value.toString());
        return instance;
    }

    private CoreInstance buildJavaEnum(Enum<?> enumValue)
    {
        CoreInstance instance = newCoreInstance(JAVA_ENUM_CLASS);
        setProperty(instance, M3Properties._class, enumValue.getDeclaringClass().getName());
        setProperty(instance, M3Properties.name, enumValue.name());
        return instance;
    }

    private CoreInstance buildJavaMap(Map<?, ?> map)
    {
        MutableList<CoreInstance> keyValuePairs = Lists.mutable.ofInitialCapacity(map.size());
        map.forEach((key, value) ->
        {
            CoreInstance convertedKey = buildJavaValue(key);
            CoreInstance convertedValue = buildJavaValue(value);

            CoreInstance pair = newCoreInstance(M3Paths.Pair);
            setProperty(pair, M3Properties.first, convertedKey);
            setProperty(pair, M3Properties.second, convertedValue);
            keyValuePairs.add(pair);
        });
        CoreInstance instance = newCoreInstance(JAVA_MAP_CLASS);
        setProperty(instance, M3Properties._class, map.getClass().getName());
        setProperty(instance, "keyValuePairs", keyValuePairs);
        return instance;
    }

    private CoreInstance buildJavaIterable(Iterable<?> iterable)
    {
        MutableList<CoreInstance> values = Iterate.collect(iterable, this::buildJavaValue, Lists.mutable.empty());
        CoreInstance instance = newCoreInstance(JAVA_ITERABLE_CLASS);
        setProperty(instance, M3Properties._class, iterable.getClass().getName());
        setProperty(instance, M3Properties.values, values);
        return instance;
    }

    private CoreInstance buildJavaOtherObject(Object object)
    {
        CoreInstance instance = newCoreInstance(JAVA_OBJECT_CLASS);
        setProperty(instance, M3Properties._class, object.getClass().getName());
        setProperty(instance, "string", object.toString());
        return instance;
    }

    private void setProperty(CoreInstance instance, String property, String value)
    {
        setProperty(instance, property, newStringCoreInstance(value));
    }

    private void setProperty(CoreInstance instance, String property, CoreInstance value)
    {
        Instance.setValueForProperty(instance, property, value, this.processorSupport);
    }

    private void setProperty(CoreInstance instance, String property, ListIterable<? extends CoreInstance> values)
    {
        Instance.setValuesForProperty(instance, property, values, this.processorSupport);
    }

    private CoreInstance newBooleanCoreInstance(boolean value)
    {
        return newCoreInstance(Boolean.toString(value), M3Paths.Boolean);
    }

    private CoreInstance newStringCoreInstance(String string)
    {
        return newCoreInstance(string, M3Paths.String);
    }

    private CoreInstance newCoreInstance(String typeName)
    {
        return newCoreInstance(null, typeName);
    }

    private CoreInstance newCoreInstance(String name, String typeName)
    {
        return this.processorSupport.newCoreInstance(name, typeName, null);
    }
}
