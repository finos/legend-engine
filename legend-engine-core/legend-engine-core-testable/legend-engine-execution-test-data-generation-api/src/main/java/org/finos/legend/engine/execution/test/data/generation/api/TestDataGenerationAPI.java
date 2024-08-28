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

package org.finos.legend.engine.execution.test.data.generation.api;

import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.execution.test.data.generation.SeedDataGeneration;
import org.finos.legend.engine.execution.test.data.generation.TestDataGeneration;
import org.finos.legend.engine.execution.test.data.generation.api.model.SeedDataGenerationInput;
import org.finos.legend.engine.execution.test.data.generation.api.model.TestDataGenerationWithDefaultSeedInput;
import org.finos.legend.engine.execution.test.data.generation.api.model.TestDataGenerationWithSeedInput;
import org.finos.legend.engine.plan.execution.PlanExecutor;

import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.plan.execution.planHelper.PrimitiveValueSpecificationToObjectVisitor;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ParameterValue;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.Map;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Api(tags = "Query - Pure")
@Path("pure/v1/execution")
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class TestDataGenerationAPI
{
    private final ModelManager modelManager;
    private final PlanExecutor planExecutor;

    @Inject
    public TestDataGenerationAPI(ModelManager modelManager, PlanExecutor planExecutor)
    {
        this.modelManager = modelManager;
        this.planExecutor = planExecutor;
    }

    @POST
    @Path("testDataGeneration/generateSeedData")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response seed_data_generate(@Context HttpServletRequest request, SeedDataGenerationInput seedDataGenerationInput, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        Identity identity = Identity.makeIdentity(profiles);
        try (Scope scope = GlobalTracer.get().buildSpan("Service: Seed Data Generation").startActive(true))
        {
            Map<String, Object> parameterNameValueMap = Maps.mutable.empty();
            if (seedDataGenerationInput.parameterValues != null)
            {
                for (ParameterValue parameterValue : seedDataGenerationInput.parameterValues)
                {
                    parameterNameValueMap.put(parameterValue.name, parameterValue.value.accept(new PrimitiveValueSpecificationToObjectVisitor()));
                }
            }
            String clientVersion = seedDataGenerationInput.clientVersion == null ? PureClientVersions.production : seedDataGenerationInput.clientVersion;
            return SeedDataGeneration.executeSeedDataGenerate(pureModel -> HelperValueSpecificationBuilder.buildLambda(seedDataGenerationInput.function.body, seedDataGenerationInput.function.parameters, pureModel.getContext()),
                    () -> modelManager.loadModel(seedDataGenerationInput.model, clientVersion, identity, null),
                    seedDataGenerationInput.mapping,
                    seedDataGenerationInput.runtime,
                    seedDataGenerationInput.context,
                    parameterNameValueMap,
                    clientVersion,
                    identity, request.getRemoteUser(),
                    this.planExecutor
            );
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.EXECUTE_INTERACTIVE_ERROR, identity.getName());
        }
    }

    @POST
    @Path("testDataGeneration/generateTestData_WithSeed")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.TEXT_PLAIN)
    public Response test_data_generate_with_seed(@Context HttpServletRequest request, TestDataGenerationWithSeedInput testDataGenerationInput, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        Identity identity = Identity.makeIdentity(profiles);
        try (Scope scope = GlobalTracer.get().buildSpan("Service: Test Data Generation").startActive(true))
        {
            String clientVersion = testDataGenerationInput.clientVersion == null ? PureClientVersions.production : testDataGenerationInput.clientVersion;
            Map<String, Object> parameterNameValueMap = Maps.mutable.empty();
            if (testDataGenerationInput.parameterValues != null)
            {
                for (ParameterValue parameterValue : testDataGenerationInput.parameterValues)
                {
                    parameterNameValueMap.put(parameterValue.name, parameterValue.value.accept(new PrimitiveValueSpecificationToObjectVisitor()));
                }
            }
            return TestDataGeneration.executeTestDataGenerateWithSeed(
                    pureModel -> HelperValueSpecificationBuilder.buildLambda(testDataGenerationInput.function.body, testDataGenerationInput.function.parameters, pureModel.getContext()),
                    () -> modelManager.loadModel(testDataGenerationInput.model, clientVersion, identity, null),
                    testDataGenerationInput.mapping,
                    testDataGenerationInput.runtime,
                    testDataGenerationInput.context,
                    testDataGenerationInput.tableRowIdentifiers,
                    testDataGenerationInput.hashStrings,
                    parameterNameValueMap,
                    clientVersion,
                    identity,
                    request.getRemoteUser(),
                    this.planExecutor
            );
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.GENERATE_TEST_DATA_ERROR, identity.getName());
        }
    }

    @POST
    @Path("testDataGeneration/generateTestData_WithDefaultSeed")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.TEXT_PLAIN)
    public Response test_data_generate_with_default_seed(@Context HttpServletRequest request, TestDataGenerationWithDefaultSeedInput testDataGenerationInput, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        Identity identity = Identity.makeIdentity(profiles);
        try (Scope scope = GlobalTracer.get().buildSpan("Service: Test Data Generation").startActive(true))
        {
            String clientVersion = testDataGenerationInput.clientVersion == null ? PureClientVersions.production : testDataGenerationInput.clientVersion;
            Map<String, Object> parameterNameValueMap = Maps.mutable.empty();
            if (testDataGenerationInput.parameterValues != null)
            {
                for (ParameterValue parameterValue : testDataGenerationInput.parameterValues)
                {
                    parameterNameValueMap.put(parameterValue.name, parameterValue.value.accept(new PrimitiveValueSpecificationToObjectVisitor()));
                }
            }
            return TestDataGeneration.executeTestDataGenerateWithDefaultSeedUtil(
                    pureModel -> HelperValueSpecificationBuilder.buildLambda(testDataGenerationInput.function.body, testDataGenerationInput.function.parameters, pureModel.getContext()),
                    () -> modelManager.loadModel(testDataGenerationInput.model, clientVersion, identity, null),
                    testDataGenerationInput.mapping,
                    testDataGenerationInput.runtime,
                    testDataGenerationInput.context,
                    testDataGenerationInput.hashStrings,
                    parameterNameValueMap,
                    clientVersion,
                    identity,
                    request.getRemoteUser(),
                    this.planExecutor
            );
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.GENERATE_TEST_DATA_ERROR, identity.getName());
        }
    }
}