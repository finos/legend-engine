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

package org.finos.legend.engine.query.pure.api.test.inMemory;

import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.stores.inMemory.plugin.InMemory;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.LegacyRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.JsonModelConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.XmlModelConnection;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedFunction;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.classInstance.ClassInstance;
import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.PackageableElementPtr;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.BaseExecutionContext;
import org.finos.legend.engine.protocol.pure.dsl.graph.valuespecification.constant.classInstance.PropertyGraphFetchTree;
import org.finos.legend.engine.protocol.pure.dsl.graph.valuespecification.constant.classInstance.RootGraphFetchTree;
import org.finos.legend.engine.query.pure.api.Execute;
import org.finos.legend.engine.shared.core.api.model.ExecuteInput;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.url.EngineUrlStreamHandlerFactory;
import org.finos.legend.pure.generated.core_java_platform_binding_legendJavaPlatformBinding_store_m2m_m2mLegendJavaPlatformBindingExtension;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class TestM2MGrammarCompileAndExecute
{
    private static final String GRAPH_FETCH = "graphFetch_T_MANY__RootGraphFetchTree_1__T_MANY_";
    private static final String SERIALIZE = "serialize_T_MANY__RootGraphFetchTree_1__String_1_";
    private static final String GET_ALL = "getAll_Class_1__T_MANY_";

    @BeforeClass
    public static void setUpUrls()
    {
        EngineUrlStreamHandlerFactory.initialize();
    }

    @Test
    public void testM2MSimpleStringJoin()
    {
        PureModelContextData contextData = PureGrammarParser.newInstance().parseModel("" +
                "Class test::Person\n" +
                "{\n" +
                "   fullName: String[1];\n" +
                "}" +
                "\n" +
                "Class test::S_Person\n" +
                "{\n" +
                "   firstName: String[1];\n" +
                "   lastName: String[1];\n" +
                "}" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::combineNames\n" +
                "(\n" +
                "   *test::Person[test_Person] : Pure\n" +
                "            {\n" +
                "               ~src test::S_Person\n" +
                "               fullName : $src.firstName + ' ' + $src.lastName\n" +
                "            }\n" +
                ")\n"
        );

        ClassInstance fetchTree = rootGFT("test::Person");
        LambdaFunction lambda = lambda(apply(SERIALIZE, apply(GRAPH_FETCH, apply(GET_ALL, clazz("test::Person")), fetchTree), fetchTree));

        ExecuteInput input = new ExecuteInput();
        input.clientVersion = "vX_X_X";
        input.model = contextData;
        input.mapping = "test::combineNames";
        input.function = lambda;
        input.runtime = runtimeValue(jsonModelConnection("test::S_Person", "{\"firstName\":\"Jane\",\"lastName\":\"Doe\"}"));
        input.context = context();
        runTest(input);
    }

    @Test
    public void testM2MBigDecimalValue() throws IOException
    {
        PureModelContextData contextData = PureGrammarParser.newInstance().parseModel("" +
                "Class test::A\n" +
                "{\n" +
                "   d: Decimal[1];\n" +
                "}" +
                "\n" +
                "Class test::S_A\n" +
                "{\n" +
                "   d: Decimal[1];\n" +
                "}" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::decimalMapping\n" +
                "(\n" +
                "   *test::A : Pure\n" +
                "            {\n" +
                "               ~src test::S_A\n" +
                "               d : $src.d\n" +
                "            }\n" +
                ")\n"
        );

        ClassInstance fetchTree = rootGFT("test::A", propertyGFT("d"));
        LambdaFunction lambda = lambda(apply(SERIALIZE, apply(GRAPH_FETCH, apply(GET_ALL, clazz("test::A")), fetchTree), fetchTree));

        ExecuteInput input = new ExecuteInput();
        input.clientVersion = "vX_X_X";
        input.model = contextData;
        input.mapping = "test::decimalMapping";
        input.function = lambda;
        input.runtime = runtimeValue(jsonModelConnection("test::S_A", "{\"d\": 999999999999999999.9333339999}"));
        input.context = context();
        String json = responseAsString(runTest(input));
        assertEquals("{\"builder\":{\"_type\":\"json\"},\"values\":{\"d\":999999999999999999.9333339999}}", json);
    }

    @Test
    public void testM2MGraphWithAssociations()
    {
        PureModelContextData contextData = PureGrammarParser.newInstance().parseModel("" +
                "Class test::Company\n" +
                "{\n" +
                "    name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::Employee\n" +
                "{\n" +
                "    fullName : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::Firm\n" +
                "{\n" +
                "    name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::Person\n" +
                "{\n" +
                "    firstName : String[1];\n" +
                "    lastName : String[1];\n" +
                "}\n" +
                "\n" +
                "Association test::Company_Employee\n" +
                "{\n" +
                "    company : test::Company[1];\n" +
                "    employees : test::Employee[*];\n" +
                "}\n" +
                "\n" +
                "Association test::FirmPerson\n" +
                "{\n" +
                "    firm : test::Firm[1];\n" +
                "    people : test::Person[*];\n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::FirmToCompany\n" +
                "(\n" +
                "   *test::Company[test_Company] : Pure\n" +
                "    {\n" +
                "     ~src test::Firm\n" +
                "     name: $src.name,\n" +
                "     employees[test_Employee]: $src.people\n" +
                "    }\n" +
                "\n" +
                "   *test::Employee[test_Employee] : Pure\n" +
                "    {\n" +
                "     ~src test::Person\n" +
                "     fullName: $src.firstName + ' ' + $src.lastName\n" +
                "    }\n" +
                ")"
        );

        ClassInstance fetchTree = rootGFT("test::Company", propertyGFT("name"), propertyGFT("employees", propertyGFT("fullName")));
        LambdaFunction lambda = lambda(apply(SERIALIZE, apply(GRAPH_FETCH, apply(GET_ALL, clazz("test::Company")), fetchTree), fetchTree));

        ExecuteInput input = new ExecuteInput();
        input.clientVersion = "vX_X_X";
        input.model = contextData;
        input.mapping = "test::FirmToCompany";
        input.function = lambda;
        input.runtime = runtimeValue(jsonModelConnection("test::Firm", "{\"name\":\"Metalurgy Inc\", \"people\":[{\"firstName\":\"Jane\",\"lastName\":\"Doe\"},{\"firstName\":\"Johny\",\"lastName\":\"Doe\"}]}\""));
        input.context = context();
        runTest(input);
    }

    @Test
    public void testHandlesDerived() throws IOException
    {
        String derivedPure;
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(TestM2MGrammarCompileAndExecute.class.getResourceAsStream("derived.pure"))))
        {
            derivedPure = buffer.lines().collect(Collectors.joining("\n"));
        }

        PureModelContextData contextData = PureGrammarParser.newInstance().parseModel(derivedPure);

        ClassInstance fetchTree = rootGFT("test::FirstEmployee", propertyGFT("name"));
        LambdaFunction lambda = lambda(apply(SERIALIZE, apply(GRAPH_FETCH, apply(GET_ALL, clazz("test::FirstEmployee")), fetchTree), fetchTree));

        ExecuteInput input = new ExecuteInput();
        input.clientVersion = "vX_X_X";
        input.model = contextData;
        input.mapping = "test::m1";
        input.function = lambda;
        input.runtime = runtimeValue(jsonModelConnection("test::Firm", "{\"name\": \"firm1\", \"employees\": [{\"firstName\": \"Jane\", \"lastName\": \"Doe\"}]}"));
        input.context = context();

        String json = responseAsString(runTest(input));
        assertEquals("{\"builder\":{\"_type\":\"json\"},\"values\":{\"name\":\"Doe\"}}", json);
    }

    @Test
    public void testM2MEnumMappingWithSpecialCharacters() throws IOException
    {
        PureModelContextData contextData = PureGrammarParser.newInstance().parseModel("" +
                "Class test::SourceClass\n" +
                "{\n" +
                "   enum : test::SourceEnum[1];\n" +
                "}" +
                "\n" +
                "Class test::TargetClass\n" +
                "{\n" +
                "   enum: test::TargetEnum[1];\n" +
                "}" +
                "\n" +
                "Enum test::SourceEnum\n" +
                "{\n" +
                "   'Source Enum@$#_*[]{}()'\n" +
                "}" +
                "\n" +
                "Enum test::TargetEnum\n" +
                "{\n" +
                "   'Target Enum@$#_*[]{}()'\n" +
                "}" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::testMapping\n" +
                "(\n" +
                "   test::TargetClass : Pure\n" +
                "   {\n" +
                "     ~src test::SourceClass\n" +
                "     enum : EnumerationMapping enumMapping : $src.enum\n" +
                "   }\n" +
                "   test::TargetEnum : EnumerationMapping enumMapping\n" +
                "   {\n" +
                "     'Target Enum@$#_*[]{}()' : [test::SourceEnum.'Source Enum@$#_*[]{}()'] \n" +
                "   }\n" +
                ")\n"
        );

        ClassInstance fetchTree = rootGFT("test::TargetClass", propertyGFT("enum"));
        LambdaFunction lambda = lambda(apply(SERIALIZE, apply(GRAPH_FETCH, apply(GET_ALL, clazz("test::TargetClass")), fetchTree), fetchTree));

        ExecuteInput jsonInput = new ExecuteInput();
        jsonInput.clientVersion = "vX_X_X";
        jsonInput.model = contextData;
        jsonInput.mapping = "test::testMapping";
        jsonInput.function = lambda;
        jsonInput.runtime = runtimeValue(jsonModelConnection("test::SourceClass", "{\"enum\": \"Source Enum@$#_*[]{}()\"}"));
        jsonInput.context = context();
        String jsonResult1 = responseAsString(runTest(jsonInput));
        assertEquals("{\"builder\":{\"_type\":\"json\"},\"values\":{\"enum\":\"Target Enum@$#_*[]{}()\"}}", jsonResult1);

        String xmlSourceData =
                "<SourceClass>" +
                "<enum>Source Enum@$#_*[]{}()</enum>" +
                "</SourceClass>";
        ExecuteInput input = new ExecuteInput();
        input.clientVersion = "vX_X_X";
        input.model = contextData;
        input.mapping = "test::testMapping";
        input.function = lambda;
        input.runtime = runtimeValue(xmlModelConnection("test::SourceClass", xmlSourceData));
        input.context = context();
        String jsonResult2 = responseAsString(runTest(input));
        assertEquals("{\"builder\":{\"_type\":\"json\"},\"values\":{\"enum\":\"Target Enum@$#_*[]{}()\"}}", jsonResult2);
    }

    @Test
    public void testM2MIndexOfMapsToInteger() throws IOException
    {
        // Regression test for the M2M Java code-generation bug where the Pure j_invoke return
        // type declared for indexOf was the element type of the collection (e.g. String) instead
        // of an integer. Mapping the result into an Integer-typed property failed to compile the
        // generated Java. See essential/collection.pure (indexOf fc2).
        PureModelContextData contextData = PureGrammarParser.newInstance().parseModel("" +
                "Class test::S_Item\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::Item\n" +
                "{\n" +
                "   name : String[1];\n" +
                "   idx  : Integer[0..1];\n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::itemMapping\n" +
                "(\n" +
                "   *test::Item : Pure\n" +
                "   {\n" +
                "     ~src test::S_Item\n" +
                "     name : $src.name,\n" +
                "     idx  : ['a', 'b', 'c']->indexOf($src.name)\n" +
                "   }\n" +
                ")\n"
        );

        ClassInstance fetchTree = rootGFT("test::Item", propertyGFT("name"), propertyGFT("idx"));
        LambdaFunction lambda = lambda(apply(SERIALIZE, apply(GRAPH_FETCH, apply(GET_ALL, clazz("test::Item")), fetchTree), fetchTree));

        ExecuteInput input = new ExecuteInput();
        input.clientVersion = "vX_X_X";
        input.model = contextData;
        input.mapping = "test::itemMapping";
        input.function = lambda;
        input.runtime = runtimeValue(jsonModelConnection("test::S_Item", "{\"name\":\"b\"}"));
        input.context = context();
        String json = responseAsString(runTest(input));
        assertEquals("{\"builder\":{\"_type\":\"json\"},\"values\":{\"name\":\"b\",\"idx\":1}}", json);
    }

    @Test
    public void testM2MDowncastCollectionElementsToSubtype() throws IOException
    {
        // Regression for castCoder in essential/lang.pure: whole-list downcast followed by property navigation.
        PureModelContextData contextData = PureGrammarParser.newInstance().parseModel("" +
                "Class test::S_Box\n" +
                "{\n" +
                "   kind  : String[1];\n" +
                "   items : test::S_Shape[*];\n" +
                "}\n" +
                "Class test::S_Shape\n" +
                "{\n" +
                "}\n" +
                "Class test::S_Circle extends test::S_Shape\n" +
                "{\n" +
                "   label : String[1];\n" +
                "}\n" +
                "Class test::T_Row\n" +
                "{\n" +
                "   labels : String[*];\n" +
                "}\n" +
                "function test::circleLabels(box: test::S_Box[1]): String[*]\n" +
                "{\n" +
                "   if($box.kind == 'c', |$box.items->cast(@test::S_Circle).label, |[])\n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::TestMapping\n" +
                "(\n" +
                "   *test::T_Row : Pure\n" +
                "   {\n" +
                "     ~src test::S_Box\n" +
                "     labels : $src->test::circleLabels()\n" +
                "   }\n" +
                ")\n"
        );

        ClassInstance fetchTree = rootGFT("test::T_Row", propertyGFT("labels"));
        LambdaFunction lambda = lambda(apply(SERIALIZE, apply(GRAPH_FETCH, apply(GET_ALL, clazz("test::T_Row")), fetchTree), fetchTree));

        ExecuteInput input = new ExecuteInput();
        input.clientVersion = "vX_X_X";
        input.model = contextData;
        input.mapping = "test::TestMapping";
        input.function = lambda;
        input.runtime = runtimeValue(jsonModelConnection("test::S_Box",
                "{\"kind\":\"c\",\"items\":[{\"@type\":\"S_Circle\",\"label\":\"alpha\"},{\"@type\":\"S_Circle\",\"label\":\"beta\"}]}"));
        input.context = context();
        String json = responseAsString(runTest(input));
        assertEquals("{\"builder\":{\"_type\":\"json\"},\"values\":{\"labels\":[\"alpha\",\"beta\"]}}", json);
    }

    @Test
    public void testM2MNestedMatchDefaultArms() throws IOException
    {
        PureModelContextData contextData = PureGrammarParser.newInstance().parseModel("" +
                "Class test::Src\n" +
                "{\n" +
                "   items : test::Item[*];\n" +
                "}\n" +
                "Class test::Item\n" +
                "{\n" +
                "}\n" +
                "Class test::ItemA extends test::Item\n" +
                "{\n" +
                "}\n" +
                "Class test::Tgt\n" +
                "{\n" +
                "   count : Integer[0..1];\n" +
                "}\n" +
                "function test::classify(s: test::Src[1]): Integer[1]\n" +
                "{\n" +
                "   $s.items->at(0)->match([\n" +
                "      a1: test::ItemA[1] | 10,\n" +
                "      default: Any[*] | $s.items->map(c | $c->match([ a2: test::ItemA[1] | 20, default: Any[*] | 0 ]))->at(0)\n" +
                "   ])\n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::TestMapping\n" +
                "(\n" +
                "   *test::Tgt : Pure\n" +
                "   {\n" +
                "     ~src test::Src\n" +
                "     count : $src->test::classify()\n" +
                "   }\n" +
                ")\n"
        );

        ClassInstance fetchTree = rootGFT("test::Tgt", propertyGFT("count"));
        LambdaFunction lambda = lambda(apply(SERIALIZE, apply(GRAPH_FETCH, apply(GET_ALL, clazz("test::Tgt")), fetchTree), fetchTree));

        ExecuteInput input = new ExecuteInput();
        input.clientVersion = "vX_X_X";
        input.model = contextData;
        input.mapping = "test::TestMapping";
        input.function = lambda;
        input.runtime = runtimeValue(jsonModelConnection("test::Src", "{\"items\":[{\"@type\":\"ItemA\"}]}"));
        input.context = context();
        String json = responseAsString(runTest(input));
        assertEquals("{\"builder\":{\"_type\":\"json\"},\"values\":{\"count\":10}}", json);
    }

    @Test
    public void testM2MNestedLambdaParamNameCollidingWithCalleeFunctionParam() throws IOException
    {
        PureModelContextData contextData = PureGrammarParser.newInstance().parseModel("" +
                "Class test::Swap\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "Class test::SrcHolder\n" +
                "{\n" +
                "   swaps : test::Swap[*];\n" +
                "}\n" +
                "Class test::TgtRow\n" +
                "{\n" +
                "   label : String[0..1];\n" +
                "}\n" +
                "function test::first(swap: test::Swap[1]): String[1]\n" +
                "{\n" +
                "   $swap.name\n" +
                "}\n" +
                "function test::labelOf(h: test::SrcHolder[1]): String[0..1]\n" +
                "{\n" +
                "   $h.swaps->map(swap | $h.swaps->map(swap | $swap->test::first()))->first()\n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::TestMapping\n" +
                "(\n" +
                "   *test::TgtRow : Pure\n" +
                "   {\n" +
                "     ~src test::SrcHolder\n" +
                "     label : $src->test::labelOf()\n" +
                "   }\n" +
                ")\n"
        );

        ClassInstance fetchTree = rootGFT("test::TgtRow", propertyGFT("label"));
        LambdaFunction lambda = lambda(apply(SERIALIZE, apply(GRAPH_FETCH, apply(GET_ALL, clazz("test::TgtRow")), fetchTree), fetchTree));

        ExecuteInput input = new ExecuteInput();
        input.clientVersion = "vX_X_X";
        input.model = contextData;
        input.mapping = "test::TestMapping";
        input.function = lambda;
        input.runtime = runtimeValue(jsonModelConnection("test::SrcHolder", "{\"swaps\":[{\"name\":\"A\"},{\"name\":\"B\"}]}"));
        input.context = context();
        String json = responseAsString(runTest(input));
        assertEquals("{\"builder\":{\"_type\":\"json\"},\"values\":{\"label\":\"A\"}}", json);
    }

    private Response runTest(ExecuteInput input)
    {
        ModelManager modelManager = new ModelManager(DeploymentMode.TEST);
        PlanExecutor executor = PlanExecutor.newPlanExecutor(InMemory.build());
        //Should use: core_pure_extensions_extension.Root_meta_pure_extension_defaultExtensions__Extension_MANY_(modelManager.)
        Response result = new Execute(modelManager, executor, (PureModel pureModel) -> core_java_platform_binding_legendJavaPlatformBinding_store_m2m_m2mLegendJavaPlatformBindingExtension.Root_meta_pure_mapping_modelToModel_executionPlan_platformBinding_legendJava_inMemoryExtensionsWithLegendJavaPlatformBinding__Extension_MANY_(pureModel.getExecutionSupport()), LegendPlanTransformers.transformers).execute(TestExecutionUtility.buildMockRequest(), input, SerializationFormat.defaultFormat, null, null);
        Assert.assertEquals(200, result.getStatus());
        return result;
    }

    private String responseAsString(Response response) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamingOutput output = (StreamingOutput) response.getEntity();
        output.write(baos);
        return baos.toString("UTF-8");
    }

    private PackageableElementPtr clazz(String fullPath)
    {
        PackageableElementPtr clazz = new PackageableElementPtr();
        clazz.fullPath = fullPath;
        return clazz;
    }

    private LambdaFunction lambda(ValueSpecification body)
    {
        LambdaFunction lambda = new LambdaFunction();
        lambda.body = Collections.singletonList(body);
        return lambda;
    }

    private AppliedFunction apply(String fControl, ValueSpecification... parameters)
    {
        AppliedFunction apply = new AppliedFunction();
        apply.fControl = fControl;
        apply.function = fControl.substring(0, fControl.indexOf('_'));
        apply.parameters = Arrays.asList(parameters);
        return apply;
    }

    private ClassInstance rootGFT(String clazz, PropertyGraphFetchTree... subTrees)
    {
        RootGraphFetchTree fetchTree = new RootGraphFetchTree();
        fetchTree._class = clazz;
        fetchTree.subTrees = Arrays.asList(subTrees);
        return new ClassInstance("rootGraphFetchTree", fetchTree, fetchTree.sourceInformation);
    }

    private PropertyGraphFetchTree propertyGFT(String property, PropertyGraphFetchTree... subTrees)
    {
        PropertyGraphFetchTree fetchTree = new PropertyGraphFetchTree();
        fetchTree.property = property;
        fetchTree.subTrees = Arrays.asList(subTrees);
        return fetchTree;
    }

    private JsonModelConnection jsonModelConnection(String clazz, String data)
    {
        JsonModelConnection connection = new JsonModelConnection();
        connection.element = "ModelStore";
        connection._class = clazz;
        connection.url = "data:application/json," + data;
        return connection;
    }

    private XmlModelConnection xmlModelConnection(String clazz, String data)
    {
        XmlModelConnection connection = new XmlModelConnection();
        connection.element = "ModelStore";
        connection._class = clazz;
        connection.url = "data:application/xml," + data;
        return connection;
    }

    private BaseExecutionContext context()
    {
        BaseExecutionContext context = new BaseExecutionContext();
        context.enableConstraints = true;
        return context;
    }

    private Runtime runtimeValue(Connection connection)
    {
        LegacyRuntime runtime = new LegacyRuntime();
        runtime.connections = Collections.singletonList(connection);
        return runtime;
    }
}
