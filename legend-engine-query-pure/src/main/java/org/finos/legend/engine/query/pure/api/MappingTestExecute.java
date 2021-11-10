// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.query.pure.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.MappingTest;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.model.ExecuteMappingTestInput;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler;
import org.finos.legend.engine.test.runner.mapping.MappingTestRunner;
import org.finos.legend.engine.test.runner.mapping.RichMappingTestResult;
import org.finos.legend.pure.generated.Root_meta_pure_router_extension_RouterExtension;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;
import org.slf4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Api(tags = "Pure - Execution")
@Path("pure/v1/execution")
@Produces(MediaType.APPLICATION_JSON)
public class MappingTestExecute
{
    private final ModelManager modelManager;
    private final PlanExecutor planExecutor;
    private Function<PureModel, RichIterable<? extends Root_meta_pure_router_extension_RouterExtension>> extensions;
    private MutableList<PlanTransformer> transformers;
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");

    public MappingTestExecute(ModelManager modelManager, PlanExecutor planExecutor, Function<PureModel, RichIterable<? extends Root_meta_pure_router_extension_RouterExtension>> extensions, MutableList<PlanTransformer> transformers)
    {
        this.modelManager = modelManager;
        this.planExecutor = planExecutor;
        this.extensions = extensions;
        this.transformers = transformers;
        MetricsHandler.createMetrics(this.getClass());
    }

    @POST
    @ApiOperation(value = "Execute a Pure query present in the mapping Test(function) in the context of a Mapping and a Runtime. Full Interactive and Semi Interactive modes are supported by giving the appropriate PureModelContext (respectively PureModelDataContext and PureModelContextComposite). Production executions need to use the Service interface.")
    @Path("doMappingTest")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response execute(@Context HttpServletRequest request, ExecuteMappingTestInput executeInput, @DefaultValue(SerializationFormat.defaultFormatString) @QueryParam("serializationFormat") SerializationFormat format, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        try (Scope scope = GlobalTracer.get().buildSpan("Service: Mapping Test Execution").startActive(true)) {
            long start = System.currentTimeMillis();
            String clientVersion = executeInput.clientVersion == null ? PureClientVersions.production : executeInput.clientVersion;
            PureModel pureModel = modelManager.loadModel(executeInput.model, clientVersion, profiles, null);
            List<RichMappingTestResult> resultList;
            List<MappingTest> mappingTestList = getAllMappingTests(executeInput, (PureModelContextData) executeInput.model);
            LOGGER.info(new LogInfo(profiles, LoggingEventType.EXECUTE_INTERACTIVE_START, "").toString());
            if(executeInput.testIdList == null || executeInput.testIdList.size() == 0)
            {
                resultList = mappingTestList.stream().map(test -> new MappingTestRunner((PureModelContextData) executeInput.model, pureModel, executeInput.mapping, test, this.planExecutor, this.extensions.apply(pureModel), this.transformers, executeInput.clientVersion).setupAndRunTest()).collect(Collectors.toList());
            }
            else
            {
                resultList = executeInput.testIdList.stream().map(testId -> new MappingTestRunner((PureModelContextData) executeInput.model, pureModel, executeInput.mapping, mappingTestList.stream().filter(x -> testId.contains(x.name)).findFirst().get(), this.planExecutor, this.extensions.apply(pureModel), this.transformers, executeInput.clientVersion).setupAndRunTest()).collect(Collectors.toList());
            }
            LOGGER.info(new LogInfo(profiles, LoggingEventType.EXECUTE_INTERACTIVE_STOP, (double)System.currentTimeMillis() - start).toString());
            return Response.ok().entity(objectMapper.writeValueAsString(resultList)).build();
        }
        catch (Exception ex)
        {
            MetricsHandler.observeError("MappingTestExecute");
            return ExceptionTool.exceptionManager(ex, LoggingEventType.EXECUTE_INTERACTIVE_ERROR, profiles);
        }
    }

    private List<MappingTest> getAllMappingTests(ExecuteMappingTestInput executeInput, PureModelContextData contextData)
    {
        return contextData.getElementsOfType(Mapping.class).stream().filter(x -> x.getPath().equals(executeInput.mapping)).findFirst().get().tests;
    }
}
