// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.runtime.java.extension.external.json.natives;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTestToJson extends AbstractPureTestWithCoreCompiled
{
    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.delete("/test/testFile.pure");
        runtime.compile();
    }

    /*
     * Functional unit tests for basic functionality
     */

    @Test
    public void testSerializationPrimitiveTypes()
    {
        compileTestSource("fromString.pure",
                "import meta::json::*;\n" +
                        "function meta::pure::functions::asserts::assertJsonStringsEqual(expected:String[1], actual:String[1]):Boolean[1]\n" +
                        "{\n" +
                        "    assert(equalJsonStrings($expected,$actual), | 'JSON strings don\\'t represent semantically same object \\n expected: ' + $expected + '\\n actual: ' + $actual);\n" +
                        "}\n" +
                        "function test():Boolean[1]\n" +
                        "{\n" +
                        "   let t = ^test::AllPrimitiveProperties(stringType='Yoda', integerType=42, floatType=3.14159265358979,\n" +
                        "                                         booleanType=false, dateType=date(2018), strictDateType=date(2018,4,12),\n" +
                        "                                         dateTimeType=date(2018,4,12,0,0,1), decimalType=3.14159265358979d);" +
                        "   assertJsonStringsEqual('{\"stringType\":\"Yoda\",\"dateTimeType\":\"2018-04-12T00:00:01+0000\",\"booleanType\":false,\"strictDateType\":\"2018-04-12\",\"floatType\":3.14159265358979,\"dateType\":\"2018\",\"integerType\":42,\"decimalType\":3.14159265358979}', $t->toJsonBeta(^meta::json::JSONSerializationConfig(typeKeyName='__TYPE', includeType=false, fullyQualifiedTypePath=false, serializeQualifiedProperties=false, serializePackageableElementName=false, removePropertiesWithEmptyValues=false)));\n" +
                        "}\n");
        execute("test():Boolean[1]");
    }

    @Test
    public void testSerializationEnumProperty()
    {
        compileTestSource("fromString.pure",
                "import meta::json::*;\n" +
                        "function test():Boolean[1]\n" +
                        "{\n" +
                        "   let t = ^test::WithEnumProperty(enumProperty=test::SomeEnum.M);\n" +
                        "let expected1 = '{\"simple\":[],\"enumProperty\":\"M\"}';\n" +
                        "let expected2 = '{\"enumProperty\":\"M\",\"simple\":[]}';\n" +
                        "assert($t->toJsonBeta(^meta::json::JSONSerializationConfig(typeKeyName='__TYPE', includeType=false, fullyQualifiedTypePath=false, serializeQualifiedProperties=false, serializePackageableElementName=false, removePropertiesWithEmptyValues=false)) == $expected1 || $t->toJsonBeta(^meta::json::JSONSerializationConfig(typeKeyName='__TYPE', includeType=false, fullyQualifiedTypePath=false, serializeQualifiedProperties=false, serializePackageableElementName=false, removePropertiesWithEmptyValues=false)) == $expected2, |'');\n" +
                        "}");
        execute("test():Boolean[1]");
    }

    @Test
    public void testSerializationClassProperty()
    {
        compileTestSource("fromString.pure",
                "import meta::json::*;\n" +
                        "function test():Boolean[1]\n" +
                        "{\n" +
                        "   let t = ^test::OuterClass(nestedClassProperty=^test::WithEnumProperty(enumProperty=test::SomeEnum.M));\n" +
                        "let expected1 = '{\"nestedClassProperty\":{\"simple\":[],\"enumProperty\":\"M\"}}';\n" +
                        "let expected2 = '{\"nestedClassProperty\":{\"enumProperty\":\"M\",\"simple\":[]}}';\n" +
                        "assert($t->toJsonBeta(^meta::json::JSONSerializationConfig(typeKeyName='__TYPE', includeType=false, fullyQualifiedTypePath=false, serializeQualifiedProperties=false, serializePackageableElementName=false, removePropertiesWithEmptyValues=false)) == $expected1 || $t->toJsonBeta(^meta::json::JSONSerializationConfig(typeKeyName='__TYPE', includeType=false, fullyQualifiedTypePath=false, serializeQualifiedProperties=false, serializePackageableElementName=false, removePropertiesWithEmptyValues=false)) == $expected2, |'');\n" +
                        "}");
        execute("test():Boolean[1]");
    }

    @Test
    public void testSerializationClassWithAssociation()
    {
        compileTestSource("fromString.pure",
                "import meta::json::*;\n" +
                        "function test():Boolean[1]\n" +
                        "{\n" +
                        "   let t = ^test::Class1(prop=[42, 1], c2=^test::Class2(str='Hey'));\n" +
                        "let expected1 = '{\"c2\":{\"str\":\"Hey\"},\"prop\":[42,1]}';\n" +
                        "let expected2 = '{\"prop\":[42,1],\"c2\":{\"str\":\"Hey\"}}';\n" +
                        "assert($t->toJsonBeta(^meta::json::JSONSerializationConfig(typeKeyName='__TYPE', includeType=false, fullyQualifiedTypePath=false, serializeQualifiedProperties=false, serializePackageableElementName=false, removePropertiesWithEmptyValues=false)) == $expected1 || $t->toJsonBeta(^meta::json::JSONSerializationConfig(typeKeyName='__TYPE', includeType=false, fullyQualifiedTypePath=false, serializeQualifiedProperties=false, serializePackageableElementName=false, removePropertiesWithEmptyValues=false)) == $expected2, |'');\n" +
                        "}");
        execute("test():Boolean[1]");
    }

    @Test
    public void testSerializationMapWithPrimitiveValues()
    {
        compileTestSource("/test/testFile.pure",
                "import test::*;\n" +
                        "function test::runTest():Any[*]\n" +
                        "{\n" +
                        "   newMap([pair('a', 1), pair('b', 2), pair('c', 3)])->meta::json::toJsonBeta(^meta::json::JSONSerializationConfig(typeKeyName='__TYPE', includeType=false, fullyQualifiedTypePath=false, serializeQualifiedProperties=false, serializePackageableElementName=false, removePropertiesWithEmptyValues=false));\n" +
                        "}\n");
        CoreInstance result = execute("test::runTest():Any[*]");
        String json = PrimitiveUtilities.getStringValue(result.getValueForMetaPropertyToOne(M3Properties.values));
        assertJsonEquals("{\"a\":1, \"b\":2, \"c\":3}", json);
    }

    @Test
    public void testSerializationMapWithNonPrimitiveValue()
    {
        compileTestSource("/test/testFile.pure",
                "import test::*;\n" +
                        "function test::runTest():Any[*]\n" +
                        "{\n" +
                        "   newMap([pair('a', ^X(prop='b')), pair('c', ^X(prop='d'))])->meta::json::toJsonBeta(^meta::json::JSONSerializationConfig(typeKeyName='__TYPE', includeType=false, fullyQualifiedTypePath=false, serializeQualifiedProperties=false, serializePackageableElementName=false, removePropertiesWithEmptyValues=false));\n" +
                        "}\n" +
                        "\n" +
                        "Class X\n" +
                        "{\n" +
                        "  prop:String[1];\n" +
                        "}\n");
        CoreInstance result = execute("test::runTest():Any[*]");
        String json = PrimitiveUtilities.getStringValue(result.getValueForMetaPropertyToOne(M3Properties.values));
        assertJsonEquals("{\"a\":{\"prop\":\"b\"}, \"c\":{\"prop\":\"d\"}}", json);
    }

    @Test
    public void testSerializationNestedMap()
    {
        compileTestSource("/test/testFile.pure",
                "import test::*;\n" +
                        "function test::runTest():Any[*]\n" +
                        "{\n" +
                        "   newMap([pair('a', newMap([pair('b', 2), pair('c', 3)])),\n" +
                        "           pair('d', newMap([pair('e', 4), pair('f', 5)]))])->meta::json::toJsonBeta(^meta::json::JSONSerializationConfig(typeKeyName='__TYPE', includeType=false, fullyQualifiedTypePath=false, serializeQualifiedProperties=false, serializePackageableElementName=false, removePropertiesWithEmptyValues=false));\n" +
                        "}\n");
        CoreInstance result = execute("test::runTest():Any[*]");
        String json = PrimitiveUtilities.getStringValue(result.getValueForMetaPropertyToOne(M3Properties.values));
        assertJsonEquals("{\"a\":{\"b\":2, \"c\":3}, \"d\":{\"e\":4, \"f\":5}}", json);
    }

    @Test
    public void testSerializationMapWithNonPrimitiveValueInClass()
    {
        compileTestSource("/test/testFile.pure",
                "import test::*;\n" +
                        "function test::runTest():Any[*]\n" +
                        "{\n" +
                        "   let x = ^A(z = 'a',y=newMap([pair('ash',^N(u='n'))]));\n" +
                        "   $x->meta::json::toJsonBeta(^meta::json::JSONSerializationConfig(typeKeyName='__TYPE', includeType=false, fullyQualifiedTypePath=false, serializeQualifiedProperties=false, serializePackageableElementName=false, removePropertiesWithEmptyValues=false));\n" +
                        "}\n" +
                        "\n" +
                        "Class test::A\n" +
                        "{\n" +
                        "   z: String[0..1];\n" +
                        "   y: Map<String,N>[0..1];\n" +
                        "}\n" +
                        "\n" +
                        "Class N\n" +
                        "{\n" +
                        "   u: String[0..1];\n" +
                        "}");
        CoreInstance result = execute("test::runTest():Any[*]");
        String json = PrimitiveUtilities.getStringValue(result.getValueForMetaPropertyToOne(M3Properties.values));
        assertJsonEquals("{\"y\":{\"ash\":{\"u\":\"n\"}},\"z\":\"a\"}", json);
    }

    @Test
    public void testSerializationUnitInstance()
    {
        String massDefinition =
                "Measure pkg::Mass\n" +
                        "{\n" +
                        "   *Gram: x -> $x;\n" +
                        "   Kilogram: x -> $x*1000;\n" +
                        "   Pound: x -> $x*453.59;\n" +
                        "}";

        compileTestSource("fromString.pure",
                "import pkg::*;\n" +
                        massDefinition +
                        "function testUnitToJson():Any[*]\n" +
                        "{\n" +
                        "   let res = 5 Mass~Kilogram->meta::json::toJsonBeta(^meta::json::JSONSerializationConfig(typeKeyName='__TYPE', includeType=false, fullyQualifiedTypePath=false, serializeQualifiedProperties=false, serializePackageableElementName=false, removePropertiesWithEmptyValues=false));\n" +
                        "}\n");
        CoreInstance result = execute("testUnitToJson():Any[*]");
        String json = PrimitiveUtilities.getStringValue(result.getValueForMetaPropertyToOne(M3Properties.values));
        assertJsonEquals("{\"unit\":[{\"unitId\":\"pkg::Mass~Kilogram\",\"exponentValue\":1}],\"value\":5}", json);
    }

    @Test
    public void testSerializationClassWithUnitInstanceAsProperty()
    {
        String massDefinition =
                "Measure pkg::Mass\n" +
                        "{\n" +
                        "   *Gram: x -> $x;\n" +
                        "   Kilogram: x -> $x*1000;\n" +
                        "   Pound: x -> $x*453.59;\n" +
                        "}";

        compileTestSource("fromString.pure",
                "import pkg::*;\n" +
                        massDefinition +
                        "Class A\n" +
                        "{\n" +
                        "myWeight : Mass~Kilogram[1];\n" +
                        "}\n" +
                        "function testUnitToJson():Any[*]\n" +
                        "{\n" +
                        "   let res = ^A(myWeight=5 Mass~Kilogram)->meta::json::toJsonBeta(^meta::json::JSONSerializationConfig(typeKeyName='__TYPE', includeType=false, fullyQualifiedTypePath=false, serializeQualifiedProperties=false, serializePackageableElementName=false, removePropertiesWithEmptyValues=false));\n" +
                        "}\n");
        CoreInstance result = execute("testUnitToJson():Any[*]");
        String json = PrimitiveUtilities.getStringValue(result.getValueForMetaPropertyToOne(M3Properties.values));
        assertJsonEquals("{\"myWeight\":{\"unit\":[{\"unitId\":\"pkg::Mass~Kilogram\",\"exponentValue\":1}],\"value\":5}}", json);
    }

    @Test
    public void testSerializationClassWithUnitInstanceAsMultiplicityManyProperty()
    {
        String massDefinition =
                "Measure pkg::Mass\n" +
                        "{\n" +
                        "   *Gram: x -> $x;\n" +
                        "   Kilogram: x -> $x*1000;\n" +
                        "   Pound: x -> $x*453.59;\n" +
                        "}";

        compileTestSource("fromString.pure",
                "import pkg::*;\n" +
                        massDefinition +
                        "Class A\n" +
                        "{\n" +
                        "myWeight : Mass~Kilogram[*];\n" +
                        "}\n" +
                        "function testUnitToJson():Any[*]\n" +
                        "{\n" +
                        "   let res = ^A(myWeight=[5 Mass~Kilogram, 1 Mass~Kilogram])->meta::json::toJsonBeta(^meta::json::JSONSerializationConfig(typeKeyName='__TYPE', includeType=false, fullyQualifiedTypePath=false, serializeQualifiedProperties=false, serializePackageableElementName=false, removePropertiesWithEmptyValues=false));\n" +
                        "}\n");
        CoreInstance result = execute("testUnitToJson():Any[*]");
        String json = PrimitiveUtilities.getStringValue(result.getValueForMetaPropertyToOne(M3Properties.values));
        assertJsonEquals("{\"myWeight\":[{\"unit\":[{\"unitId\":\"pkg::Mass~Kilogram\",\"exponentValue\":1}],\"value\":5},{\"unit\":[{\"unitId\":\"pkg::Mass~Kilogram\",\"exponentValue\":1}],\"value\":1}]}", json);
    }

    @Test
    public void testSerializationClassWithUnitInstanceAsPropertyRemoveOptionalUnitProperty()
    {
        String massDefinition =
                "Measure pkg::Mass\n" +
                        "{\n" +
                        "   *Gram: x -> $x;\n" +
                        "   Kilogram: x -> $x*1000;\n" +
                        "   Pound: x -> $x*453.59;\n" +
                        "}";

        compileTestSource("fromString.pure",
                "import pkg::*;\n" +
                        massDefinition +
                        "Class A\n" +
                        "{\n" +
                        "myWeight : Mass~Kilogram[1];\n" +
                        "}\n" +
                        "function testUnitToJsonWithType():Any[*]\n" +
                        "{\n" +
                        "   let res = ^A(myWeight=5 Mass~Kilogram)->meta::json::toJsonBeta(^meta::json::JSONSerializationConfig(typeKeyName='__TYPE', includeType=true, removePropertiesWithEmptyValues=true));\n" +
                        "}\n");
        CoreInstance result = execute("testUnitToJsonWithType():Any[*]");
        String json = PrimitiveUtilities.getStringValue(result.getValueForMetaPropertyToOne(M3Properties.values));
        assertJsonEquals("{\"__TYPE\":\"A\",\"myWeight\":{\"unit\":[{\"unitId\":\"pkg::Mass~Kilogram\",\"exponentValue\":1}],\"value\":5}}", json);
    }

    @Test
    public void testSerializationClassWithUnitInstanceAsPropertyNotRemoveOptionalProperty()
    {
        String massDefinition =
                "Measure pkg::Mass\n" +
                        "{\n" +
                        "   *Gram: x -> $x;\n" +
                        "   Kilogram: x -> $x*1000;\n" +
                        "   Pound: x -> $x*453.59;\n" +
                        "}";

        compileTestSource("fromString.pure",
                "import pkg::*;\n" +
                        massDefinition +
                        "Class A\n" +
                        "{\n" +
                        "myWeight : Mass~Kilogram[1];\n" +
                        "myOptionalWeight : Mass~Kilogram[0..1];\n" +
                        "}\n" +
                        "function testUnitToJsonWithType():Any[*]\n" +
                        "{\n" +
                        "   let res = ^A(myWeight=5 Mass~Kilogram)->meta::json::toJsonBeta(^meta::json::JSONSerializationConfig(typeKeyName='__TYPE', includeType=true, removePropertiesWithEmptyValues=false));\n" +
                        "}\n");
        CoreInstance resultTwo = execute("testUnitToJsonWithType():Any[*]");
        String jsonTwo = PrimitiveUtilities.getStringValue(resultTwo.getValueForMetaPropertyToOne(M3Properties.values));
        assertJsonEquals("{\"__TYPE\":\"A\",\"myWeight\":{\"unit\":[{\"unitId\":\"pkg::Mass~Kilogram\",\"exponentValue\":1}],\"value\":5},\"myOptionalWeight\":[]}", jsonTwo);
    }

    @Test
    public void testSerializationKilogramTypeAsPackageableElement()
    {
        String massDefinition =
                "Measure pkg::Mass\n" +
                        "{\n" +
                        "   *Gram: x -> $x;\n" +
                        "   Kilogram: x -> $x*1000;\n" +
                        "   Pound: x -> $x*453.59;\n" +
                        "}";

        compileTestSource("fromString.pure",
                "import pkg::*;\n" +
                        massDefinition +
                        "function testSerializeKilogramType():Any[*]\n" +
                        "{\n" +
                        "   let res = Mass~Kilogram->meta::json::toJsonBeta(^meta::json::JSONSerializationConfig(typeKeyName='__TYPE', serializePackageableElementName=true));\n" +
                        "}\n");
        CoreInstance result = execute("testSerializeKilogramType():Any[*]");
        String json = PrimitiveUtilities.getStringValue(result.getValueForMetaPropertyToOne(M3Properties.values));
        assertJsonEquals("\"pkg::Mass~Kilogram\"", json);
    }

    private void assertJsonEquals(String expectedJson, String actualJson)
    {
        Object expected;
        Object actual;
        try
        {
            expected = JSONValue.parseWithException(expectedJson);
        }
        catch (ParseException e)
        {
            throw new RuntimeException("Error parsing expected JSON: " + expectedJson, e);
        }
        try
        {
            actual = JSONValue.parseWithException(actualJson);
        }
        catch (ParseException e)
        {
            throw new RuntimeException("Error parsing actual JSON: " + actualJson, e);
        }
        Assert.assertEquals(expected, actual);
    }

    protected static MutableRepositoryCodeStorage getCodeStorage()
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        CodeRepositorySet.Builder builder = CodeRepositorySet.newBuilder().withCodeRepositories(CodeRepositoryProviderHelper.findCodeRepositories(classLoader, true));
        MutableList<CodeRepository> rep = Lists.mutable.withAll(builder.build().getRepositories());
        rep.add(new GenericCodeRepository("test", null, "platform", "core_functions_unclassified", "core_functions_json"));
        return new CompositeCodeStorage(new ClassLoaderCodeStorage(rep));
    }

    public static Pair<String, String> getExtra()
    {
        String code = readTextResource("org/finos/legend/pure/m3/toJson/toJson.pure", org.finos.legend.pure.m3.tests.function.base.json.AbstractTestToJson.class.getClassLoader());
        return Tuples.pair("/test/toJson.pure", code);
    }
}
