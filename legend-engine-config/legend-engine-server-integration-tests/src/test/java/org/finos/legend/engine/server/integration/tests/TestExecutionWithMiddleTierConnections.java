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
import org.finos.legend.engine.plan.execution.api.ExecutePlan;
import org.finos.legend.engine.plan.execution.authorization.RelationalMiddleTierPlanExecutionAuthorizer;
import org.finos.legend.engine.plan.execution.authorization.RootMiddleTierPlanExecutionAuthorizer;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.identity.factory.DefaultIdentityFactory;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestExecutionWithMiddleTierConnections extends AbstractMiddleTierExecutionTest
{
    @Test
    public void testWithExecutePlan_ExecutionAuthorized() throws Exception
    {
        // fake the credential authorization - allow credential use
        AlwaysAllowCredentialAuthorizer credentialAuthorizer = new AlwaysAllowCredentialAuthorizer();
        RelationalMiddleTierPlanExecutionAuthorizer relationalMiddleTierPlanExecutionAuthorizer = new RelationalMiddleTierPlanExecutionAuthorizer(credentialAuthorizer);
        RootMiddleTierPlanExecutionAuthorizer planExecutionAuthorizer = new RootMiddleTierPlanExecutionAuthorizer(Lists.immutable.of(relationalMiddleTierPlanExecutionAuthorizer));

        SingleExecutionPlan executionPlan = this.loadPlanFromFile("/plans/planWithSingleMiddleTierConnection.json", postgresTestContainerWrapper.getPort());
        Response response = new ExecutePlan(buildPlanExecutor(), planExecutionAuthorizer, new DefaultIdentityFactory()).doExecutePlanImpl(executionPlan, SerializationFormat.defaultFormat, profiles);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String expectedOutput = "{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\"\"}], \"result\" : {\"columns\" : [\"fullName\"], \"rows\" : [{\"values\": [\"Mickey Mouse\"]}]}}";
        assertEquals(expectedOutput, this.readResponse(response));
    }

    @Test
    public void testWithExecutePlan_ExecutionNotAuthorized() throws Exception
    {
        // fake the credential authorization - deny credential use
        AlwaysDenyCredentialAuthorizer credentialAuthorizer = new AlwaysDenyCredentialAuthorizer();
        RelationalMiddleTierPlanExecutionAuthorizer relationalMiddleTierPlanExecutionAuthorizer = new RelationalMiddleTierPlanExecutionAuthorizer(credentialAuthorizer);
        RootMiddleTierPlanExecutionAuthorizer planExecutionAuthorizer = new RootMiddleTierPlanExecutionAuthorizer(Lists.immutable.of(relationalMiddleTierPlanExecutionAuthorizer));

        SingleExecutionPlan executionPlan = this.loadPlanFromFile("/plans/planWithSingleMiddleTierConnection.json", postgresTestContainerWrapper.getPort());
        Response response = new ExecutePlan(buildPlanExecutor(), planExecutionAuthorizer, new DefaultIdentityFactory()).doExecutePlanImpl(executionPlan, SerializationFormat.defaultFormat, profiles);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());

        String expectedOutput = "{\"authorizer\":\"RelationalMiddleTierPlanExecutionAuthorizer\",\"authorized\":false,\"summary\":\"Overall authorization was NOT successful. Authorizations granted=0, Authorizations denied=1\",\"authorizationInput\":{\"contextParams\":{\"legend.usageContext\":\"INTERACTIVE_EXECUTION\"}},\"authorizations\":[{\"status\":\"DENY\",\"summary\":\"Use of credential denied by policy\",\"subject\":\"_UNKNOWN_\",\"action\":\"use\",\"resource\":{\"credential\":\"reference1\"},\"policyParams\":{\"policy\":\"unused\",\"resource\":\"DB@test@localhost@64514\"},\"details\":[]}]}";
        assertTrue(this.readResponseError(response).contains("\"Overall authorization was NOT successful. Authorizations granted=0, Authorizations denied=1\""));
    }

    private SingleExecutionPlan loadPlanFromFile(String planResourceName, int portNumber) throws IOException, URISyntaxException
    {
        URL resource = TestExecutionWithMiddleTierConnections.class.getResource(planResourceName);
        String planString = new String(Files.readAllBytes(Paths.get(resource.toURI())), Charset.defaultCharset());
        planString = planString.replaceAll("__PORT_NUMBER__", String.valueOf(portNumber));
        return ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(planString, SingleExecutionPlan.class);
    }
}