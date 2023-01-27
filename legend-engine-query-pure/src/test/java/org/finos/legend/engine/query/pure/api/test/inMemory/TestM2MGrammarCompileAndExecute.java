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
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.ClassInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PackageableElementPtr;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.BaseExecutionContext;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.graph.PropertyGraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.graph.RootGraphFetchTree;
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
        Lambda lambda = lambda(apply(SERIALIZE, apply(GRAPH_FETCH, apply(GET_ALL, clazz("test::Person")), fetchTree), fetchTree));

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
        Lambda lambda = lambda(apply(SERIALIZE, apply(GRAPH_FETCH, apply(GET_ALL, clazz("test::A")), fetchTree), fetchTree));

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
        Lambda lambda = lambda(apply(SERIALIZE, apply(GRAPH_FETCH, apply(GET_ALL, clazz("test::Company")), fetchTree), fetchTree));

        ExecuteInput input = new ExecuteInput();
        input.clientVersion = "vX_X_X";
        input.model = contextData;
        input.mapping = "test::FirmToCompany";
        input.function = lambda;
        input.runtime = runtimeValue(jsonModelConnection("test::Firm", "{\"name\":\"Metalurgy Inc\", \"people\":[{\"firstName\":\"Jane\",\"lastName\":\"Doe\"},{\"firstName\":\"Johny\",\"lastName\":\"Doe\"}]}\""));
        input.context = context();
        runTest(input);
    }

    //TODO- move these to pure m2m tests
    @Test
    public void testM2MGraph_RootSubtypes_singleSubtype() throws IOException
    {
        runM2MGraphSubtypes(" test::withSubType::rootLevel::sourceRoot::testMappingWithSingleSubType");
    }

    @Test
    public void testM2MGraph_RootSubtypes_instanceOf() throws IOException
    {
        runM2MGraphSubtypes("test::withSubType::rootLevel::sourceRoot::testMappingWithMultipleSubTypes_instanceOf");
    }

    @Test
    public void testM2MGraph_RootSubtypes_match() throws IOException
    {
        runM2MGraphSubtypes("test::withSubType::rootLevel::sourceRoot::testMappingWithMultipleSubTypes_match");
    }

    @Test
    public void testM2MGraph_RootSubtypes_match_functionCall() throws IOException
    {
        runM2MGraphSubtypes("test::withSubType::rootLevel::sourceRoot::testMappingWithMultipleSubTypes_match_functionCall");
    }

    public void runM2MGraphSubtypes(String mappingName) throws IOException
    {
         String modelCode =
                 "import test::withSubType::*;\n" +
                 "Class test::withSubType::Target\n" +
                 "{\n" +
                 "   targetZipCode: String[1];\n" +
                 "   targetAddress: String[1];\n" +
                 "}\n" +
                 " \n" +
                 "Class test::withSubType::SourceClass\n" +
                 "{\n" +
                 "   sourceZipCode: String[1];\n" +
                 "   sourceAddress: String[1];\n" +
                 "}\n" +
                 "\n" +
                 "Class test::withSubType::Person\n" +
                 "{\n" +
                 "   address: Location[1];\n" +
                 "}\n" +
                 " \n" +
                 "Class test::withSubType::Location\n" +
                 "{\n" +
                 "   zipCode: String[1];\n" +
                 "   coordinates: String[1];\n" +
                 "}\n" +
                 " \n" +
                 "Class test::withSubType::Street extends Location\n" +
                 "{\n" +
                 "   street: String[1];\n" +
                 "}\n" +
                 " \n" +
                 "Class test::withSubType::Road extends Location\n" +
                 "{\n" +
                 "   road: String[1];\n" +
                 "}\n" +
                 "\n" +
                 "Class test::withSubType::TargetStreetCluster\n" +
                 "{\n" +
                 "  streetNames: String[*];\n" +
                 "  zipCodes: String[*];\n" +
                 "}\n" +
                 "\n" +
                 "Class test::withSubType::SourceStreetCluster\n" +
                 "{\n" +
                 "  streetCluster: Street[*];\n" +
                 "}\n" +
                 "\n" +
                 "Class test::withSubType::TargetPerson\n" +
                 "{\n" +
                 "   address: TargetLocation[1];\n" +
                 "}\n" +
                 "\n" +
                 "Class test::withSubType::TargetLocation\n" +
                 "{\n" +
                 "   zipCode: String[1];\n" +
                 "   coordinates: String[1];\n" +
                 "}\n" +
                 " \n" +
                 "Class test::withSubType::TargetStreet extends TargetLocation\n" +
                 "{\n" +
                 "   street: String[1];\n" +
                 "}\n" +
                 " \n" +
                 "Class test::withSubType::TargetRoad extends TargetLocation\n" +
                 "{\n" +
                 "   road: String[1];\n" +
                 "}\n" +
                 "function test::withSubType::rootLevel::sourceRoot::getLocationStr(loc:test::withSubType::Location[1]):String[1]\n" +
                 "{\n" +
                 "  $loc->match([\n" +
                 "         s:test::withSubType::Street[1] | $s.street, \n" +
                 "         r:test::withSubType::Road[1] | $r.road,\n" +
                 "         l:test::withSubType::Location[1] | $l.coordinates\n" +
                 "         ]);\n" +
                 "}\n" +
                 "\n" +
                 "###Mapping\n" +
                 "import test::withSubType::*;\n" +
                 "Mapping test::withSubType::rootLevel::sourceRoot::testMappingWithSingleSubType\n" +
                 "(\n" +
                 "   *test::withSubType::Target: Pure\n" +
                 "   {\n" +
                 "      ~src test::withSubType::Location\n" +
                 "      targetZipCode: $src.zipCode,\n" +
                 "      targetAddress: if($src->instanceOf(test::withSubType::Street),|$src->cast(@test::withSubType::Street).street,|'unknown')\n" +
                 "   }\n" +
                 ")\n" +
                 "\n" +
                 "Mapping test::withSubType::rootLevel::sourceRoot::testMappingWithMultipleSubTypes_instanceOf\n" +
                 "(\n" +
                 "   *test::withSubType::Target: Pure\n" +
                 "   {\n" +
                 "      ~src test::withSubType::Location\n" +
                 "      targetZipCode: $src.zipCode,\n" +
                 "      targetAddress:if($src->instanceOf(test::withSubType::Street),\n" +
                 "                       |$src->cast(@test::withSubType::Street).street,\n" +
                 "                       |\n" +
                 "                         if($src->instanceOf(test::withSubType::Road),\n" +
                 "                         |$src->cast(@test::withSubType::Road).road,\n" +
                 "                         |$src->cast(@test::withSubType::Location).coordinates\n" +
                 "                         )\n" +
                 "                      )   \n" +
                 "   }\n" +
                 ")\n" +
                 "Mapping test::withSubType::rootLevel::sourceRoot::testMappingWithMultipleSubTypes_match\n" +
                 "(\n" +
                 "   *test::withSubType::Target: Pure\n" +
                 "   {\n" +
                 "      ~src test::withSubType::Location\n" +
                 "      targetZipCode: $src.zipCode,\n" +
                 "      targetAddress: $src->match([\n" +
                 "         s:Street[1] | $s.street, \n" +
                 "         r:Road[1] | $r.road,\n" +
                 "         l:Location[1] | $l.coordinates\n" +
                 "         ])\n" +
                 "   }\n" +
                 ")\n" +
                 "Mapping test::withSubType::rootLevel::sourceRoot::testMappingWithMultipleSubTypes_match_functionCall\n" +
                 "(\n" +
                 "   *test::withSubType::Target: Pure\n" +
                 "   {\n" +
                 "      ~src test::withSubType::Location\n" +
                 "      targetZipCode: $src.zipCode,\n" +
                 "      targetAddress: $src->test::withSubType::rootLevel::sourceRoot::getLocationStr()\n" +
                 "      }\n" +
                 ")\n";

        PureModelContextData contextData = PureGrammarParser.newInstance().parseModel(modelCode);

        ClassInstance fetchTree = rootGFT("test::withSubType::Target", propertyGFT("targetZipCode"), propertyGFT("targetAddress"));
        Lambda lambda = lambda(apply(SERIALIZE, apply(GRAPH_FETCH, apply(GET_ALL, clazz("test::withSubType::Target")), fetchTree), fetchTree));

        ExecuteInput input = new ExecuteInput();
        input.clientVersion = "vX_X_X";
        input.model = contextData;
        input.mapping = "test::withSubType::rootLevel::sourceRoot::testMappingWithMultipleSubTypes_match";
        input.function = lambda;
        input.runtime = runtimeValue(jsonModelConnection("test::withSubType::Location",
                 "[{ \"zipCode\": \"10000000\", \"coordinates\": \"111.1111\"}, \n" +
                         "{ \"zipCode\": \"20000000\", \"coordinates\": \"222.2222\" , \"street\" : \"myStreet\" , \"@type\":\"test::withSubType::Street\"}, \n" +
                         "{ \"zipCode\": \"30000000\", \"coordinates\": \"333.333\" , \"road\" : \"myRoad\" , \"@type\":\"test::withSubType::Road\"}]"
        ));
        input.context = context();
        String json = responseAsString(runTest(input));
        assertEquals("{\"builder\":{\"_type\":\"json\"},\"values\":" +
                                        "[{\"targetZipCode\":\"10000000\",\"targetAddress\":\"111.1111\"}," +
                                        "{\"targetZipCode\":\"20000000\",\"targetAddress\":\"myStreet\"}," +
                                        "{\"targetZipCode\":\"30000000\",\"targetAddress\":\"myRoad\"}]}",
                json);
    }

    @Test
    public void testSimpleSerializeOfOneObjectWithMultiSubTypesWithoutAssoc() throws IOException
    {
        String modelCode =
                "import test::*;\n" +
                "Class test::Target\n" +
                "{\n" +
                "   targetZipCode: String[1];\n" +
                "   targetAddress: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::Person\n" +
                "{\n" +
                "   address: Location[1];\n" +
                "}\n" +
                " \n" +
                "Class test::Location\n" +
                "{\n" +
                "   zipCode: String[1];\n" +
                "   coordinates: String[1];\n" +
                "}\n" +
                " \n" +
                "Class test::Street extends Location\n" +
                "{\n" +
                "   street: String[1];\n" +
                "}\n" +
                " \n" +
                "Class test::Road extends Location\n" +
                "{\n" +
                "   road: String[1];\n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "import test::*;\n" +
                "Mapping test::testMappingWithMultipleSubTypes\n" +
                "(\n" +
                "   *test::Target: Pure\n" +
                "   {\n" +
                "      ~src test::Person\n" +
                "      targetZipCode: $src.address.zipCode,\n" +
                "      targetAddress: $src.address->match([\n" +
                "         s:Street[1] | $s.street, \n" +
                "         r:Road[1] | $r.road,\n" +
                "         l:Location[1] | $l.coordinates\n" +
                "         ])\n" +
                "   }\n" +
                ")";

        PureModelContextData contextData = PureGrammarParser.newInstance().parseModel(modelCode);

        ClassInstance fetchTree = rootGFT("test::Target", propertyGFT("targetAddress"));
        Lambda lambda = lambda(apply(SERIALIZE, apply(GRAPH_FETCH, apply(GET_ALL, clazz("test::Target")), fetchTree), fetchTree));

        ExecuteInput input = new ExecuteInput();
        input.clientVersion = "vX_X_X";
        input.model = contextData;
        input.mapping = "test::testMappingWithMultipleSubTypes";
        input.function = lambda;
        input.runtime = runtimeValue(jsonModelConnection("test::Person",
                "[{\"address\"  : [{ \"zipCode\": \"10282\", \"coordinates\": \"1\" , \"road\" : \"200 west\" , \"@type\":\"test::Road\"}]}]"
        ));
        input.context = context();
        String json = responseAsString(runTest(input));
        assertEquals("{\"builder\":{\"_type\":\"json\"},\"values\":" +
                                "{\"targetAddress\":\"200 west\"}}",
                json);
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
        Lambda lambda = lambda(apply(SERIALIZE, apply(GRAPH_FETCH, apply(GET_ALL, clazz("test::FirstEmployee")), fetchTree), fetchTree));

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

    private Lambda lambda(ValueSpecification body)
    {
        Lambda lambda = new Lambda();
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
        return new ClassInstance("rootGraphFetchTree", fetchTree);
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
