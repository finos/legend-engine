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

package org.finos.legend.engine.server.integration.tests;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.api.ExecutePlan;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.stores.inMemory.plugin.InMemory;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.Relational;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.identity.factory.DefaultIdentityFactory;
import org.finos.legend.engine.shared.core.vault.TestVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.pac4j.core.profile.CommonProfile;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class TestExecutionWithoutMiddleTierConnections
{
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
    private TestVaultImplementation testVaultImplementation;

    @Before
    public void setup() throws Exception
    {
        DefaultIdentityFactory defaultIdentityFactory = new DefaultIdentityFactory();
        this.profiles = Lists.mutable.withAll(defaultIdentityFactory.adapt(defaultIdentityFactory.makeUnknownIdentity()));

        this.testVaultImplementation = new TestVaultImplementation();
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

    private String readResponse(Response response) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamingOutput output = (StreamingOutput) response.getEntity();
        output.write(baos);
        String responseText = baos.toString("UTF-8");
        return responseText;
    }
}