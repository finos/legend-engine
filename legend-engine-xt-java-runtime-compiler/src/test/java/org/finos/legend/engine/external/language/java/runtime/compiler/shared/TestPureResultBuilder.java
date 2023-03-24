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
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.PrimitiveType;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.lang.model.SourceVersion;

public class TestPureResultBuilder extends AbstractPureTestWithCoreCompiled
{
    private static PureResultBuilder resultBuilder;

    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(new FunctionExecutionInterpreted(), getCodeStorage(), getFactoryRegistryOverride(), getOptions(), getExtra());
        runtime.loadAndCompileSystem();
        resultBuilder = new PureResultBuilder(processorSupport);
    }

    protected static MutableCodeStorage getCodeStorage()
    {
        CodeRepositorySet repositories = CodeRepositorySet.newBuilder()
                .withCodeRepositories(CodeRepositoryProviderHelper.findCodeRepositories(true))
                .build()
                .subset("core_external_language_java_compiler");

        return PureCodeStorage.createCodeStorage(null, repositories.getRepositories());
    }

    @Test
    public void testBuildJavaNull()
    {
        CoreInstance instance = resultBuilder.buildJavaValue(null);
        assertClassifierPath("meta::external::language::java::compiler::JavaNull", instance);
    }

    @Test
    public void testBuildJavaEnum()
    {
        testBuildJavaEnum(SourceVersion.RELEASE_0);
        testBuildJavaEnum(SourceVersion.RELEASE_7);
        testBuildJavaEnum(SourceVersion.RELEASE_8);
    }

    private void testBuildJavaEnum(Enum<?> javaEnum)
    {
        CoreInstance instance = resultBuilder.buildJavaValue(javaEnum);
        assertClassifierPath("meta::external::language::java::compiler::JavaEnum", instance);
        Assert.assertEquals(javaEnum.getDeclaringClass().getName(), getStringPropertyValue(instance, "class"));
        Assert.assertEquals(javaEnum.name(), getStringPropertyValue(instance, "name"));
    }

    @Test
    public void testBuildJavaArray()
    {
        testBuildJavaArray(new int[] {1, 2, 3, 4, Integer.MIN_VALUE, Integer.MAX_VALUE});
        testBuildJavaArray(new long[] {1L, 2L, 3L, 4L, Long.MIN_VALUE, Long.MAX_VALUE});
        testBuildJavaArray(new byte[] {1, 2, 3, 4, Byte.MIN_VALUE, Byte.MAX_VALUE});
        testBuildJavaArray(new short[] {1, 2, 3, 4, Short.MIN_VALUE, Short.MAX_VALUE});
        testBuildJavaArray(new char[] {'1', '2', 'a', 'b', Character.MIN_VALUE, Character.MAX_VALUE});
        testBuildJavaArray(new float[] {1.0f, 2.0f, 3.0f, 4.0f, Float.MIN_VALUE, Float.MAX_VALUE});
        testBuildJavaArray(new double[] {1.0, 2.0, 3.0, 4.0, Double.MIN_VALUE, Double.MAX_VALUE});
        testBuildJavaArray(new boolean[] {true, false});
        testBuildJavaArray(new Object[]{});
        testBuildJavaArray(new Object[]{null, "a", "b", 3L, 4, new Object[]{5, 6L, "b"}});
        testBuildJavaArray(new String[]{"the", "quick", "brown", null, "fox"});
    }

    private void testBuildJavaArray(Object array)
    {
        CoreInstance instance = resultBuilder.buildJavaValue(array);
        assertClassifierPath("meta::external::language::java::compiler::JavaArray", instance);
        Assert.assertEquals(array.getClass().getComponentType().getName(), getStringPropertyValue(instance, "componentType"));
        assertCoreInstancesEqual(buildArrayExpectedValues(array), getPropertyValues(instance, "values"));
    }

    private MutableList<CoreInstance> buildArrayExpectedValues(Object array)
    {
        Class<?> componentType = array.getClass().getComponentType();
        int length = Array.getLength(array);
        MutableList<CoreInstance> results = Lists.mutable.ofInitialCapacity(length);
        for (int i = 0; i < length; i++)
        {
            Object object = Array.get(array, i);
            CoreInstance instance;
            if (componentType.isPrimitive())
            {
                instance = processorSupport.newCoreInstance(null, "meta::external::language::java::compiler::JavaPrimitive", null);
                Instance.setValueForProperty(instance, "type", processorSupport.newCoreInstance(componentType.getName(), M3Paths.String, null), processorSupport);
                Instance.setValueForProperty(instance, "value", processorSupport.newCoreInstance(object.toString(), M3Paths.String, null), processorSupport);
            }
            else
            {
                instance = resultBuilder.buildJavaValue(object);
            }
            results.add(instance);
        }
        return results;
    }

    @Test
    public void testBuildJavaIterable()
    {
        testBuildJavaIterable(Collections.emptyList());
        testBuildJavaIterable(Lists.immutable.empty());
        testBuildJavaIterable(Lists.mutable.empty());
        testBuildJavaIterable(new ArrayList<>());
        testBuildJavaIterable(Lists.mutable.with("a", "b", "c", null, 1, 2, 3, null));

        testBuildJavaIterable(Collections.emptySet());
        testBuildJavaIterable(Sets.immutable.empty());
        testBuildJavaIterable(new HashSet<>());
        testBuildJavaIterable(Sets.mutable.with("a", "B", "c", null, 1, 2, 3, null));
    }

    private void testBuildJavaIterable(Iterable<?> iterable)
    {
        CoreInstance instance = resultBuilder.buildJavaValue(iterable);
        assertClassifierPath("meta::external::language::java::compiler::JavaIterable", instance);
        Assert.assertEquals(iterable.getClass().getName(), getStringPropertyValue(instance, "class"));
        assertCoreInstancesEqual(Iterate.collect(iterable, resultBuilder::buildJavaValue, Lists.mutable.empty()), getPropertyValues(instance, "values"));
    }

    @Test
    public void testBuildJavaMap()
    {
        testBuildJavaMap(Collections.emptyMap());
        testBuildJavaMap(new HashMap<>());
        testBuildJavaMap(Maps.immutable.empty().castToMap());
        testBuildJavaMap(Maps.mutable.with("one", 1, "two", 2));
        testBuildJavaMap(Maps.mutable.with("one", Lists.mutable.with(1, 2, 3, 4), "two", 5, "three", Sets.mutable.with(6, 7, 8)));
    }

    private void testBuildJavaMap(Map<?, ?> map)
    {
        CoreInstance instance = resultBuilder.buildJavaValue(map);
        assertClassifierPath("meta::external::language::java::compiler::JavaMap", instance);
        Assert.assertEquals(map.getClass().getName(), getStringPropertyValue(instance, "class"));

        MutableList<CoreInstance> expectedKeyValues = Lists.mutable.ofInitialCapacity(map.size());
        map.forEach((key, value) ->
        {
            CoreInstance convertedKey = resultBuilder.buildJavaValue(key);
            CoreInstance convertedValue = resultBuilder.buildJavaValue(value);
            CoreInstance pair = processorSupport.newCoreInstance(null, M3Paths.Pair, null);
            Instance.setValueForProperty(pair, M3Properties.first, convertedKey, processorSupport);
            Instance.setValueForProperty(pair, M3Properties.second, convertedValue, processorSupport);
            expectedKeyValues.add(pair);
        });
        assertCoreInstancesEqual(expectedKeyValues, getPropertyValues(instance, "keyValuePairs"));
    }

    @Test
    public void testBuildJavaOtherObject()
    {
        testBuildJavaOtherObject("the quick brown fox");
        testBuildJavaOtherObject(123);
        testBuildJavaOtherObject(123L);
        testBuildJavaOtherObject(123.0f);
        testBuildJavaOtherObject(123.0d);
        testBuildJavaOtherObject(Boolean.TRUE);
        testBuildJavaOtherObject(Boolean.FALSE);
        testBuildJavaOtherObject('\n');
        testBuildJavaOtherObject(new Object()
        {
           @Override
           public String toString()
           {
               return "expected string";
           }
        });
    }

    private void testBuildJavaOtherObject(Object object)
    {
        CoreInstance instance = resultBuilder.buildJavaValue(object);
        assertClassifierPath("meta::external::language::java::compiler::JavaObject", instance);
        Assert.assertEquals(object.getClass().getName(), getStringPropertyValue(instance, "class"));
        Assert.assertEquals(object.toString(), getStringPropertyValue(instance, "string"));
    }

    private static void assertClassifierPath(String expected, CoreInstance instance)
    {
        CoreInstance classifier = instance.getClassifier();
        Assert.assertNotNull(expected, classifier);
        String path = PackageableElement.getUserPathForPackageableElement(classifier);
        Assert.assertEquals(expected, path);
    }

    private static void assertCoreInstancesEqual(ListIterable<? extends CoreInstance> expected, ListIterable<? extends CoreInstance> actual)
    {
        Assert.assertEquals(expected.collect(TestPureResultBuilder::printCoreInstance), actual.collect(TestPureResultBuilder::printCoreInstance));
    }

    private static void assertCoreInstanceEquals(CoreInstance expected, CoreInstance actual)
    {
        Assert.assertEquals(printCoreInstance(expected), printCoreInstance(actual));
    }

    private static String printCoreInstance(CoreInstance instance)
    {
        return printCoreInstance(new StringBuilder(), instance).toString();
    }

    private static StringBuilder printCoreInstance(StringBuilder builder, CoreInstance instance)
    {
        if (instance instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement)
        {
            return PackageableElement.writeUserPathForPackageableElement(builder, instance);
        }

        CoreInstance classifier = instance.getClassifier();
        PackageableElement.writeUserPathForPackageableElement(builder.append('<'), classifier);
        if (classifier instanceof PrimitiveType)
        {
            builder.append(' ').append(instance.getName());
        }
        else
        {
            instance.getKeys().toSortedList().forEach(key ->
            {
                if (!M3Properties.classifierGenericType.equals(key))
                {
                    ListIterable<? extends CoreInstance> values = instance.getValueForMetaPropertyToMany(key);
                    if (values.notEmpty())
                    {
                        builder.append(' ').append(key).append('=');
                        if (values.size() == 1)
                        {
                            printCoreInstance(values.get(0));
                        }
                        else
                        {
                            values.forEachWithIndex((v, i) -> printCoreInstance(builder.append((i == 0) ? "[" : ", "), v));
                            builder.append(']');
                        }
                    }
                }
            });
        }
        return builder.append('>');
    }

    private static String getStringPropertyValue(CoreInstance instance, String property)
    {
        return PrimitiveUtilities.getStringValue(getPropertyValue(instance, property));
    }

    private static CoreInstance getPropertyValue(CoreInstance instance, String property)
    {
        return instance.getValueForMetaPropertyToOne(property);
    }

    private static ListIterable<? extends CoreInstance> getPropertyValues(CoreInstance instance, String property)
    {
        return instance.getValueForMetaPropertyToMany(property);
    }
}
