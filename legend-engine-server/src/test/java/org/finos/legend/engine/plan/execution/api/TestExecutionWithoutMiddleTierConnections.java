//  Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.api;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.authorization.mac.PlanExecutionAuthorizerMACKeyGenerator;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.stores.inMemory.plugin.InMemory;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.Relational;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
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
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.model.ExecuteInput;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.factory.DefaultIdentityFactory;
import org.finos.legend.engine.shared.core.vault.TestVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.pac4j.core.profile.CommonProfile;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class TestExecutionWithoutMiddleTierConnections
{
    public static final String GRAPH_FETCH = "graphFetch_T_MANY__RootGraphFetchTree_1__T_MANY_";
    public static final String SERIALIZE = "serialize_T_MANY__RootGraphFetchTree_1__String_1_";
    public static final String GET_ALL = "getAll_Class_1__T_MANY_";

    public Class clazz(String fullPath)
    {
        Class clazz = new Class();
        clazz.fullPath = fullPath;
        return clazz;
    }

    protected Lambda lambda(ValueSpecification body)
    {
        Lambda lambda = new Lambda();
        lambda.body = Collections.singletonList(body);
        return lambda;
    }

    protected AppliedFunction apply(String fControl, ValueSpecification... parameters)
    {
        AppliedFunction apply = new AppliedFunction();
        apply.fControl = fControl;
        apply.function = fControl.substring(0, fControl.indexOf('_'));
        apply.parameters = Arrays.asList(parameters);
        return apply;
    }

    protected RootGraphFetchTree rootGFT(String clazz, PropertyGraphFetchTree... subTrees)
    {
        RootGraphFetchTree fetchTree = new RootGraphFetchTree();
        fetchTree._class = clazz;
        fetchTree.subTrees = Arrays.asList(subTrees);
        return fetchTree;
    }

    protected PropertyGraphFetchTree propertyGFT(String property, PropertyGraphFetchTree... subTrees)
    {
        PropertyGraphFetchTree fetchTree = new PropertyGraphFetchTree();
        fetchTree.property = property;
        fetchTree.subTrees = Arrays.asList(subTrees);
        return fetchTree;
    }

    protected JsonModelConnection jsonModelConnection(String clazz, String data)
    {
        JsonModelConnection connection = new JsonModelConnection();
        connection.element = "ModelStore";
        connection._class = clazz;
        connection.url = "data:application/json," + data;
        return connection;
    }

    public BaseExecutionContext context()
    {
        BaseExecutionContext context = new BaseExecutionContext();
        context.enableConstraints = true;
        return context;
    }

    public Runtime runtimeValue(Connection connection)
    {
        LegacyRuntime runtime = new LegacyRuntime();
        runtime.connections = Collections.singletonList(connection);
        return runtime;
    }

    public static class ReflectiveInvocationHandler implements InvocationHandler
    {
        private final Object[] delegates;

        public ReflectiveInvocationHandler(Object... delegates)
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

    public static class Request
    {
        @SuppressWarnings("unused")
        public String getRemoteUser()
        {
            return "someone";
        }
    }

    private MutableList<CommonProfile> profiles;

    public static final String MAC_KEY_VAULT_REFERENCE = "macReference";
    private TestVaultImplementation testVaultImplementation;

    @Before
    public void setup() throws Exception
    {
        DefaultIdentityFactory defaultIdentityFactory = new DefaultIdentityFactory();
        this.profiles = Lists.mutable.withAll(defaultIdentityFactory.adapt(defaultIdentityFactory.makeUnknownIdentity()));

        this.testVaultImplementation = new TestVaultImplementation();
        testVaultImplementation.setValue(MAC_KEY_VAULT_REFERENCE, new PlanExecutionAuthorizerMACKeyGenerator().generateKeyAsString());
        Vault.INSTANCE.registerImplementation(testVaultImplementation);
    }

    @After
    public void teardown()
    {
        if (this.testVaultImplementation != null)
        {
            Vault.INSTANCE.unregisterImplementation(testVaultImplementation);
        }
    }

    @Test
    public void testWithExecutePlan() throws  Exception
    {
        SingleExecutionPlan executionPlan = this.loadPlanFromFile("/plans/planWithoutMiddleTierConnections.json");
        PlanExecutor executor = PlanExecutor.newPlanExecutor(InMemory.build(), Relational.build());
        Response response = new ExecutePlan(executor).doExecutePlanImpl(executionPlan, SerializationFormat.defaultFormat, profiles);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String responseText = this.readResponse(response);
        assertEquals("{\"builder\":{\"_type\":\"json\"},\"values\":[{\"firstName\":\"Johny\",\"lastName\":\"Doe\"},{\"firstName\":\"Jane\",\"lastName\":\"Doe\"}]}", responseText);
    }

    private SingleExecutionPlan loadPlanFromFile(String planResourceName) throws IOException, URISyntaxException
    {
        URL resource = TestExecutionWithoutMiddleTierConnections.class.getResource(planResourceName);
        String planString = new String(Files.readAllBytes(Paths.get(resource.toURI())), Charset.defaultCharset());
        return ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(planString, SingleExecutionPlan.class);
    }

    @Test
    public void testWithExecute() throws  Exception
    {
        ExecuteInput input = this.buildExecuteInput();
        execute(input);
    }

    private ExecuteInput buildExecuteInput()
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
        return input;
    }

    private Response execute(ExecuteInput input)
    {
        ModelManager modelManager = new ModelManager(DeploymentMode.TEST);
        PlanExecutor executor = PlanExecutor.newPlanExecutor(InMemory.build());
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new java.lang.Class<?>[]{HttpServletRequest.class}, new ReflectiveInvocationHandler(new Request()));
        Response result = new Execute(modelManager, executor, (PureModel pureModel) -> org.eclipse.collections.impl.factory.Lists.mutable.empty(), LegendPlanTransformers.transformers).execute(request, input, SerializationFormat.defaultFormat, null, null);
        Assert.assertEquals(200, result.getStatus());
        return result;
    }


    private String readResponse(Response response) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamingOutput output = (StreamingOutput) response.getEntity();
        output.write(baos);
        String responseText = baos.toString("UTF-8");
        return responseText;
    }
}