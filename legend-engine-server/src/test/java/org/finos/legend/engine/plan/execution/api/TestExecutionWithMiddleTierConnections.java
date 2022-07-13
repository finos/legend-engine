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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.authorization.PlanExecutionAuthorizer;
import org.finos.legend.engine.plan.execution.authorization.RelationalMiddleTierPlanExecutionAuthorizer;
import org.finos.legend.engine.plan.execution.authorization.RootMiddleTierPlanExecutionAuthorizer;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.query.pure.api.Execute;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.model.ExecuteInput;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.factory.DefaultIdentityFactory;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ServiceLoader;

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
        Response response = new ExecutePlan(buildPlanExecutor(), planExecutionAuthorizer, MAC_KEY_VAULT_REFERENCE, new DefaultIdentityFactory()).doExecutePlanImpl(executionPlan, SerializationFormat.defaultFormat, profiles);

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
        Response response = new ExecutePlan(buildPlanExecutor(), planExecutionAuthorizer, MAC_KEY_VAULT_REFERENCE, new DefaultIdentityFactory()).doExecutePlanImpl(executionPlan, SerializationFormat.defaultFormat, profiles);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());

        String expectedOutput = "{\"authorizer\":\"RelationalMiddleTierPlanExecutionAuthorizer\",\"authorized\":false,\"summary\":\"Overall authorization was NOT successful. Authorizations granted=0, Authorizations denied=1\",\"authorizationInput\":{\"contextParams\":{\"usageContext\":\"INTERACTIVE_EXECUTION\"}},\"authorizations\":[{\"status\":\"DENY\",\"summary\":\"Use of credential denied by policy\",\"subject\":\"_UNKNOWN_\",\"action\":\"use\",\"resource\":{\"credential\":\"reference1\"},\"policyParams\":{\"policy\":\"unused\",\"resource\":\"DB@test@localhost@64514\"},\"details\":[]}]}";
        assertTrue(this.readResponseError(response).contains("\"Overall authorization was NOT successful. Authorizations granted=0, Authorizations denied=1\""));
    }

    private SingleExecutionPlan loadPlanFromFile(String planResourceName, int portNumber) throws IOException, URISyntaxException
    {
        URL resource = TestExecutionWithMiddleTierConnections.class.getResource(planResourceName);
        String planString = new String(Files.readAllBytes(Paths.get(resource.toURI())), Charset.defaultCharset());
        planString = planString.replaceAll("__PORT_NUMBER__", String.valueOf(portNumber));
        return ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(planString, SingleExecutionPlan.class);
    }

    @Test
    public void testWithExecute_ExecutionAuthorized() throws Exception
    {
        // fake the credential authorization - allow credential use
        AlwaysAllowCredentialAuthorizer credentialAuthorizer = new AlwaysAllowCredentialAuthorizer();
        RelationalMiddleTierPlanExecutionAuthorizer relationalMiddleTierPlanExecutionAuthorizer = new RelationalMiddleTierPlanExecutionAuthorizer(credentialAuthorizer);
        RootMiddleTierPlanExecutionAuthorizer planExecutionAuthorizer = new RootMiddleTierPlanExecutionAuthorizer(Lists.immutable.of(relationalMiddleTierPlanExecutionAuthorizer));

        ExecuteInput input = this.buildExecuteInput(postgresTestContainerWrapper.getPort(), "/executeInputs/executeInputWithMiddleTierConnections.json");

        Response response = execute(input, buildPlanExecutor(), planExecutionAuthorizer, MAC_KEY_VAULT_REFERENCE);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        String expectedOutput = "{\"builder\": {\"_type\":\"classBuilder\",\"mapping\":\"mapping::firm\",\"classMappings\":[{\"setImplementationId\":\"domain_Person\",\"properties\":[{\"property\":\"fullName\",\"type\":\"String\"}],\"class\":\"domain::Person\"}],\"class\":\"domain::Person\"}, \"activities\": [{\"_type\":\"RelationalExecutionActivity\",\"sql\":\"select \\\"root\\\".FULLNAME as \\\"pk_0\\\", \\\"root\\\".FULLNAME as \\\"fullName\\\" from PERSON as \\\"root\\\"\"}], \"objects\" : [{\"fullName\":\"Mickey Mouse\"," +
                "\"alloyStoreObjectReference$\":\"ASOR:IGNORED\"}]}";

        // The execution output contains a dynamic store reference value.
        // For now, we crudely ignore the ASOR value when comparing actual and expected.

        String actualOutput = this.readResponse(response);
        actualOutput = actualOutput.replaceAll("\"ASOR:.*\"", "\"ASOR:IGNORED\"");
        assertEquals(expectedOutput, actualOutput);
    }

    @Test
    public void testWithExecute_ExecutionNotAuthorized() throws Exception
    {
        // fake the credential authorization - allow credential use
        AlwaysDenyCredentialAuthorizer credentialAuthorizer = new AlwaysDenyCredentialAuthorizer();
        RelationalMiddleTierPlanExecutionAuthorizer relationalMiddleTierPlanExecutionAuthorizer = new RelationalMiddleTierPlanExecutionAuthorizer(credentialAuthorizer);
        RootMiddleTierPlanExecutionAuthorizer planExecutionAuthorizer = new RootMiddleTierPlanExecutionAuthorizer(Lists.immutable.of(relationalMiddleTierPlanExecutionAuthorizer));

        ExecuteInput input = this.buildExecuteInput(postgresTestContainerWrapper.getPort(), "/executeInputs/executeInputWithMiddleTierConnections.json");

        Response response = execute(input, buildPlanExecutor(), planExecutionAuthorizer, MAC_KEY_VAULT_REFERENCE);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());

        String expectedOutput = "{\"builder\": {\"_type\":\"classBuilder\",\"mapping\":\"mapping::firm\",\"classMappings\":[{\"setImplementationId\":\"domain_Person\",\"properties\":[{\"property\":\"fullName\",\"type\":\"String\"}],\"class\":\"domain::Person\"}],\"class\":\"domain::Person\"}, \"activities\": [{\"_type\":\"RelationalExecutionActivity\",\"sql\":\"select \\\"root\\\".FULLNAME as \\\"pk_0\\\", \\\"root\\\".FULLNAME as \\\"fullName\\\" from PERSON as \\\"root\\\"\"}], \"objects\" : [{\"fullName\":\"Mickey Mouse\",\"alloyStoreObjectReference$\":\"ASOR:MDAxOjAxMDowMDAwMDAwMDEwOlJlbGF0aW9uYWw6MDAwMDAwMDAxMzptYXBwaW5nOjpmaXJtOjAwMDAwMDAwMTM6ZG9tYWluX1BlcnNvbjowMDAwMDAwMDEzOmRvbWFpbl9QZXJzb246MDAwMDAwMDMyODp7Il90eXBlIjoiUmVsYXRpb25hbERhdGFiYXNlQ29ubmVjdGlvbiIsImF1dGhlbnRpY2F0aW9uU3RyYXRlZ3kiOnsiX3R5cGUiOiJtaWRkbGVUaWVyVXNlck5hbWVQYXNzd29yZCIsInZhdWx0UmVmZXJlbmNlIjoicmVmZXJlbmNlMSJ9LCJkYXRhc291cmNlU3BlY2lmaWNhdGlvbiI6eyJfdHlwZSI6InN0YXRpYyIsImRhdGFiYXNlTmFtZSI6InRlc3QiLCJob3N0IjoibG9jYWxob3N0IiwicG9ydCI6NjUwNzB9LCJlbGVtZW50IjoiZGF0YWJhc2U6OnBnIiwicG9zdFByb2Nlc3NvcldpdGhQYXJhbWV0ZXIiOltdLCJwb3N0UHJvY2Vzc29ycyI6W10sInR5cGUiOiJQb3N0Z3JlcyJ9OjAwMDAwMDAwMjQ6eyJwayRfMCI6Ik1pY2tleSBNb3VzZSJ9\"}]}";
        assertTrue(this.readResponseError(response).contains("\"Overall authorization was NOT successful. Authorizations granted=0, Authorizations denied=1\""));
    }

    private Response execute(ExecuteInput input, PlanExecutor planExecutor, PlanExecutionAuthorizer planExecutionAuthorizer, String macKeyVaultReference) throws IOException
    {
        ModelManager modelManager = new ModelManager(DeploymentMode.TEST);

        MutableList<PlanGeneratorExtension> generatorExtensions = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
        Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions = (PureModel pureModel) -> generatorExtensions.flatCollect(e -> e.getExtraExtensions(pureModel));

        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{HttpServletRequest.class}, new ReflectiveInvocationHandler(new Request()));
        Response result = new Execute(modelManager, planExecutor, routerExtensions, generatorExtensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers),
                planExecutionAuthorizer, macKeyVaultReference, new DefaultIdentityFactory())
                .execute(request, input, SerializationFormat.defaultFormat, null, null);
        return result;
    }

    private ExecuteInput buildExecuteInput(int port, String executeInputResourceName) throws Exception
    {
        URL resource = TestExecutionWithMiddleTierConnections.class.getResource(executeInputResourceName);
        String executeInputAsString = new String(Files.readAllBytes(Paths.get(resource.toURI())), Charset.defaultCharset());
        executeInputAsString = executeInputAsString.replaceAll("__PORT_NUMBER__", String.valueOf(port));

        ExecuteInput executeInput = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(executeInputAsString, ExecuteInput.class);
        return  executeInput;
    }
}