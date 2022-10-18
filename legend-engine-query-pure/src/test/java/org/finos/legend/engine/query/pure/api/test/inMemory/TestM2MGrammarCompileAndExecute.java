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

import org.eclipse.collections.impl.factory.Lists;
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
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Class;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.BaseExecutionContext;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.graph.PropertyGraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.graph.RootGraphFetchTree;
import org.finos.legend.engine.query.pure.api.Execute;
import org.finos.legend.engine.shared.core.api.model.ExecuteInput;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.url.EngineUrlStreamHandlerFactory;
import org.finos.legend.pure.generated.core_pure_extensions_functions;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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

        RootGraphFetchTree fetchTree = rootGFT("test::Person");
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

        RootGraphFetchTree fetchTree = rootGFT("test::A", propertyGFT("d"));
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

        RootGraphFetchTree fetchTree = rootGFT("test::Company", propertyGFT("name"), propertyGFT("employees", propertyGFT("fullName")));
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

    @Test
    public void testHandlesDerived() throws IOException
    {
        String derivedPure;
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(TestM2MGrammarCompileAndExecute.class.getResourceAsStream("derived.pure"))))
        {
            derivedPure = buffer.lines().collect(Collectors.joining("\n"));
        }

        PureModelContextData contextData = PureGrammarParser.newInstance().parseModel(derivedPure);

        RootGraphFetchTree fetchTree = rootGFT("test::FirstEmployee", propertyGFT("name"));
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
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new java.lang.Class<?>[] {HttpServletRequest.class}, new ReflectiveInvocationHandler(new Request()));
        //Should use: core_pure_extensions_extension.Root_meta_pure_extension_defaultExtensions__Extension_MANY_(modelManager.)
        Response result = new Execute(modelManager, executor, (PureModel pureModel) -> core_pure_extensions_functions.Root_meta_pure_extension_defaultExtensions__Extension_MANY_(pureModel.getExecutionSupport()), LegendPlanTransformers.transformers).execute(request, input, SerializationFormat.defaultFormat, null, null);
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

    private Class clazz(String fullPath)
    {
        Class clazz = new Class();
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

    private RootGraphFetchTree rootGFT(String clazz, PropertyGraphFetchTree... subTrees)
    {
        RootGraphFetchTree fetchTree = new RootGraphFetchTree();
        fetchTree._class = clazz;
        fetchTree.subTrees = Arrays.asList(subTrees);
        return fetchTree;
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

    private static class ReflectiveInvocationHandler implements InvocationHandler
    {
        private final Object[] delegates;

        private ReflectiveInvocationHandler(Object... delegates)
        {
            this.delegates = delegates;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            for (Object delegate : delegates)
            {
                try
                {
                    return delegate.getClass().getMethod(method.getName(), method.getParameterTypes()).invoke(delegate, args);
                }
                catch (NoSuchMethodException e)
                {
                    // The loop will complete if all delegates fail
                }
            }
            throw new UnsupportedOperationException("Method not simulated: " + method);
        }
    }

    private static class Request
    {
        @SuppressWarnings("unused")
        public String getRemoteUser()
        {
            return "someone";
        }
    }
}
